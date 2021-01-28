package io.branchtalk

import cats.{ Functor, Monad }
import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, ExitCode, Resource, Sync, Timer }
import cats.effect.implicits._
import com.typesafe.config.ConfigFactory
import io.branchtalk.api.AppServer
import io.branchtalk.configs.{ APIConfig, AppArguments, Configuration }
import io.branchtalk.discussions.events.DiscussionEvent
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.{ ConsumerStream, DomainConfig, StreamRunner }
import io.branchtalk.shared.model.{ Logger, UUIDGenerator }
import io.branchtalk.users.{ UsersModule, UsersReads, UsersWrites }
import io.prometheus.client.CollectorRegistry
import org.http4s.metrics.prometheus.Prometheus
import sun.misc.Signal

object Program {

  implicit protected val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  def runApplication[F[_]: ConcurrentEffect: ContextShift: Timer: MDC](args: List[String]): F[ExitCode] =
    (for {
      implicit0(logger: Logger[F]) <- Logger.create[F]
      env <- Configuration.getEnv[F]
      appArguments <- AppArguments.parse[F](args, env)
      _ <- logger.info(show"Arguments passed: ${appArguments}")
      _ <-
        if (appArguments.isAnythingRun) initializeAndRunModules[F](appArguments)
        else logger.warn("Nothing to run, see --help for information how to turn on API server and projections")
    } yield ExitCode.Success).handleError {
      case noConfig @ AppArguments.NoConfig(help) =>
        if (help.errors.nonEmpty) noConfig.printError()
        else noConfig.printHelp(ConfigFactory.defaultApplication())
      case error: Throwable =>
        error.printStackTrace()
        ExitCode.Error
    }

  def resolveConfigs[F[_]: Sync](implicit logger: Logger[F]): F[(APIConfig, DomainConfig, DomainConfig)] = for {
    apiConfig <- Configuration.readConfig[F, APIConfig]("api")
    _ <- logger.info(show"App configs resolved to: ${apiConfig}")
    usersConfig <- Configuration.readConfig[F, DomainConfig]("users")
    _ <- logger.info(show"Users configs resolved to: ${usersConfig}")
    discussionsConfig <- Configuration.readConfig[F, DomainConfig]("discussions")
    _ <- logger.info(show"Discussions configs resolved to: ${discussionsConfig}")
  } yield (apiConfig, usersConfig, discussionsConfig)

  def initializeAndRunModules[F[_]: ConcurrentEffect: ContextShift: Timer: MDC](
    appArguments:    AppArguments
  )(implicit logger: Logger[F]): F[Unit] = {
    for {
      (apiConfig, usersConfig, discussionsConfig) <- Resource.liftF(resolveConfigs[F])
      registry <- Prometheus.collectorRegistry[F]
      modules <- Resource.make(logger.info("Initializing services"))(_ => logger.info("Services shut down")) >>
        (
          registry.pure[Resource[F, *]],
          UsersModule.reads[F](usersConfig, registry),
          UsersModule.writes[F](discussionsConfig, registry),
          DiscussionsModule.reads[F](discussionsConfig, registry),
          DiscussionsModule.writes[F](discussionsConfig, registry)
        ).tupled
    } yield (apiConfig, usersConfig, discussionsConfig, modules)
  }.use { case (apiConfig, usersConfig, _, modules) =>
    val run =
      runModules[F](appArguments, apiConfig, awaitTerminationSignal[F], UsersModule.listenToUsers[F](usersConfig)) _
    run.tupled(modules)
  }

  // scalastyle:off method.length
  // scalastyle:off parameter.number
  def runModules[F[_]: ConcurrentEffect: ContextShift: Timer: MDC](
    appArguments:      AppArguments,
    apiConfig:         APIConfig,
    terminationSignal: F[Unit],
    makeUsersDiscussionsConsumer: (
      ConsumerStream.Factory[F, DiscussionEvent],
      StreamRunner.FromConsumerStream[F, DiscussionEvent]
    ) => StreamRunner[F]
  )(
    registry:          CollectorRegistry,
    usersReads:        UsersReads[F],
    usersWrites:       UsersWrites[F],
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  )(implicit logger:   Logger[F]): F[Unit] = {
    (
      AppServer
        .asResource(
          appArguments = appArguments,
          apiConfig = apiConfig,
          registry = registry,
          userReads = usersReads.userReads,
          sessionReads = usersReads.sessionReads,
          banReads = usersReads.banReads,
          userWrites = usersWrites.userWrites,
          sessionWrites = usersWrites.sessionWrites,
          banWrites = usersWrites.banWrites,
          channelReads = discussionsReads.channelReads,
          postReads = discussionsReads.postReads,
          commentReads = discussionsReads.commentReads,
          subscriptionReads = discussionsReads.subscriptionReads,
          commentWrites = discussionsWrites.commentWrites,
          postWrites = discussionsWrites.postWrites,
          channelWrites = discussionsWrites.channelWrites,
          subscriptionWrites = discussionsWrites.subscriptionWrites
        )
        .void
        .conditionally("API server", appArguments.runAPI),
      // run Users projections on a separate thread
      usersWrites.runProjections.asResource.conditionally("Users' projections", appArguments.runUsersProjections),
      // run consumer on a separate thread
      makeUsersDiscussionsConsumer(discussionsReads.discussionEventConsumer,
                                   usersWrites.runDiscussionsConsumer
      ).asResource.conditionally("Users' Discussions consumer", appArguments.runUsersProjections),
      // run Users projections on a separate thread
      discussionsWrites.runProjecions.asResource.conditionally("Discussions' projections",
                                                                    appArguments.runDiscussionsProjections
      )
    ).tupled >> logBeforeAfter[F]("Services initialized", "Received exit signal")
  }.use(_ => terminationSignal) // here we are blocking until e.g. user press Ctrl+C

  // scalastyle:on parameter.number
  // scalastyle:off method.length

  // kudos to Łukasz Byczyński
  private def awaitTerminationSignal[F[_]: Concurrent]: F[Unit] = {
    def handleSignal(signalName: String): F[Unit] = Async[F].async[Unit] { cb =>
      Signal.handle(new Signal(signalName), _ => cb(().asRight[Throwable]))
      ()
    }
    handleSignal("INT").race(handleSignal("TERM")).void
  }

  private def logBeforeAfter[F[_]: Functor](before: String, after: String)(implicit logger: Logger[F]) =
    Resource.make(logger.info(before))(_ => logger.info(after))

  implicit class ResourceOps[F[_]](private val resource: Resource[F, Unit]) extends AnyVal {

    def conditionally(name: String, condition: Boolean)(implicit F: Monad[F], logger: Logger[F]): Resource[F, Unit] =
      if (condition) {
        logBeforeAfter[F](s"Starting $name", s"$name shutdown completed") >>
          resource >>
          logBeforeAfter[F](s"$name start completed", s"Shutting down $name")
      } else Resource.liftF(logger.info(s"$name disabled - omitting"))
  }
}
