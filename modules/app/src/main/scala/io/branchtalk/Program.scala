package io.branchtalk

import cats.Applicative
import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, ExitCode, Resource, Timer }
import cats.effect.implicits._
import com.typesafe.config.ConfigFactory
import io.branchtalk.api.AppServer
import io.branchtalk.configs.{ APIConfig, AppArguments, Configuration }
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import io.branchtalk.shared.model.{ Logger, UUIDGenerator }
import io.branchtalk.users.{ UsersModule, UsersReads, UsersWrites }
import io.prometheus.client.CollectorRegistry
import org.http4s.metrics.prometheus.Prometheus
import sun.misc.Signal

object Program {

  implicit protected val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  def runApplication[F[_]: ConcurrentEffect: ContextShift: Timer](args: List[String]): F[ExitCode] =
    (for {
      implicit0(logger: Logger[F]) <- Logger.create[F]
      env <- Configuration.getEnv[F]
      appArguments <- AppArguments.parse[F](args, env)
      _ <- logger.info(s"Arguments passed: ${appArguments.show}")
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

  def initializeAndRunModules[F[_]: ConcurrentEffect: ContextShift: Timer](
    appArguments:    AppArguments
  )(implicit logger: Logger[F]): F[Unit] =
    for {
      apiConfig <- Configuration.readConfig[F, APIConfig]("api")
      _ <- logger.info(s"App configs resolved to: ${apiConfig.show}")
      usersConfig <- Configuration.readConfig[F, DomainConfig]("users")
      _ <- logger.info(s"Users configs resolved to: ${usersConfig.show}")
      discussionsConfig <- Configuration.readConfig[F, DomainConfig]("discussions")
      _ <- logger.info(s"Discussions configs resolved to: ${discussionsConfig.show}")
      _ <- Prometheus
        .collectorRegistry[F]
        .flatMap { registry =>
          Resource.make(logger.info("Initializing services"))(_ => logger.info("Services shut down")) >>
            (
              registry.pure[Resource[F, *]],
              UsersModule.reads[F](usersConfig, registry),
              UsersModule.writes[F](discussionsConfig, registry),
              DiscussionsModule.reads[F](discussionsConfig, registry),
              DiscussionsModule.writes[F](discussionsConfig, registry)
            ).tupled
        }
        .use((runModules[F](appArguments, apiConfig, awaitTerminationSignal[F]) _).tupled)
    } yield ()

  // scalastyle:off number.of.parameters
  def runModules[F[_]: ConcurrentEffect: ContextShift: Timer](
    appArguments:      AppArguments,
    apiConfig:         APIConfig,
    terminationSignal: F[Unit]
  )(
    registry:          CollectorRegistry,
    usersReads:        UsersReads[F],
    usersWrites:       UsersWrites[F],
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  )(implicit logger:   Logger[F]): F[Unit] =
    (
      AppServer
        .asResource(
          appArguments = appArguments,
          apiConfig = apiConfig,
          registry = registry,
          usersReads = usersReads,
          usersWrites = usersWrites,
          discussionsReads = discussionsReads,
          discussionsWrites = discussionsWrites
        )
        .void
        .conditionally(appArguments.runAPI)(orElse = ()),
      usersWrites.runProjector.conditionally(condition = appArguments.runUsersProjections)(orElse = ().pure[F]),
      discussionsWrites.runProjector.conditionally(condition = appArguments.runDiscussionsProjections)(orElse =
        ().pure[F]
      )
    ).tupled.use { case (_, usersProjector, discussionsProjector) =>
      logger.info("Start projections if needed") >>
        usersProjector.start >> // run Users projections on a separate thread
        discussionsProjector.start >> // run Users projections on a separate thread
        logger.info("Services initialized") >>
        terminationSignal >> // here we are blocking until e.g. user press Ctrl+C
        logger.info("Received exit signal")
    }

  // kudos to Łukasz Byczyński
  private def awaitTerminationSignal[F[_]: Concurrent]: F[Unit] = {
    def handleSignal(signalName: String): F[Unit] = Async[F].async[Unit] { cb =>
      Signal.handle(new Signal(signalName), _ => cb(().asRight[Throwable]))
      ()
    }
    handleSignal("INT").race(handleSignal("TERM")).void
  }

  implicit class ResourceOps[F[_], A](private val resource: Resource[F, A]) extends AnyVal {

    def conditionally(condition: Boolean)(orElse: => A)(implicit F: Applicative[F]): Resource[F, A] =
      if (condition) resource else Resource.pure[F, A](orElse)
  }
}
