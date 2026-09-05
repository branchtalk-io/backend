package io.branchtalk

import cats.{ Functor, Monad }
import cats.effect.{ Async, ExitCode, Resource, Sync }
import cats.effect.implicits.*
import cats.effect.std.Dispatcher
import org.ekrich.config.ConfigFactory
import io.branchtalk.api.AppServer
import io.branchtalk.configs.{ APIConfig, AppArguments, Configuration }
import io.branchtalk.discussions.events.DiscussionEvent
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.logging.*
import io.branchtalk.notifications.{ NotificationsModule, NotificationsReads, NotificationsWrites }
import io.branchtalk.notifications.writes.NotificationTopic
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.{ UsersModule, UsersReads, UsersWrites }
import io.prometheus.client.CollectorRegistry
import org.http4s.metrics.prometheus.Prometheus
import sun.misc.Signal

object Program {

  protected given UUID.Generator = UUID.FastGenerator

  def runApplication[F[_]: Async: MDC](args: List[String]): F[ExitCode] =
    (for {
      given Logger[F] <- Logger.create[F]
      env <- Configuration.getEnv[F]
      appArguments <- AppArguments.parse[F](args, env)
      _ <- Logger[F].info(show"Arguments passed: $appArguments")
      _ <-
        if (appArguments.isAnythingRun) initializeAndRunModules[F](appArguments)
        else Logger[F].warn("Nothing to run, see --help for information how to turn on API server and projections")
    } yield ExitCode.Success).handleError {
      case noConfig @ AppArguments.NoConfig(help) =>
        if (help.errors.nonEmpty) noConfig.printError()
        else noConfig.printHelp(ConfigFactory.defaultApplication())
      case error: Throwable =>
        error.printStackTrace()
        ExitCode.Error
    }

  def resolveConfigs[F[_]: Sync: Logger]: F[
    (APIConfig, DomainModule.Config, DomainModule.Config, DomainModule.Config)
  ] =
    for {
      apiConfig <- Configuration.readConfig[F, APIConfig]("api")
      _ <- Logger[F].info(show"App configs resolved to: ${apiConfig}")
      usersConfig <- Configuration.readConfig[F, DomainModule.Config]("users")
      _ <- Logger[F].info(show"Users configs resolved to: ${usersConfig}")
      discussionsConfig <- Configuration.readConfig[F, DomainModule.Config]("discussions")
      _ <- Logger[F].info(show"Discussions configs resolved to: ${discussionsConfig}")
      notificationsConfig <- Configuration.readConfig[F, DomainModule.Config]("notifications")
      _ <- Logger[F].info(show"Notifications configs resolved to: ${notificationsConfig}")
    } yield (apiConfig, usersConfig, discussionsConfig, notificationsConfig)

  def initializeAndRunModules[F[_]: Async: MDC: Logger](appArguments: AppArguments): F[Unit] = {
    for {
      given Dispatcher[F] <- Dispatcher.parallel[F]
      (apiConfig, usersConfig, discussionsConfig, notificationsConfig) <- Resource.eval(resolveConfigs[F])
      registry <- Prometheus.collectorRegistry[F]
      notificationTopic <- NotificationTopic.create[F]
      modules <- Resource.make(Logger[F].info("Initializing services"))(_ => Logger[F].info("Services shut down")) >>
        (
          registry.pure[Resource[F, *]],
          UsersModule.reads[F](usersConfig, registry),
          UsersModule.writes[F](discussionsConfig, registry),
          DiscussionsModule.reads[F](discussionsConfig, registry),
          DiscussionsModule.writes[F](discussionsConfig, registry),
          NotificationsModule.reads[F](notificationsConfig, registry),
          NotificationsModule.writes[F](notificationsConfig, discussionsConfig, registry, notificationTopic)
        ).tupled
    } yield (apiConfig, usersConfig, notificationsConfig, notificationTopic, modules)
  }.use { case (apiConfig, usersConfig, notificationsConfig, notificationTopic, modules) =>
    val run =
      runModules[F](
        appArguments,
        apiConfig,
        awaitTerminationSignal[F],
        UsersModule.listenToUsers[F](usersConfig),
        NotificationsModule.listenToDiscussions[F](notificationsConfig),
        notificationTopic
      ) _
    run.tupled(modules)
  }

  def runModules[F[_]: Async: MDC: Logger](
    appArguments:      AppArguments,
    apiConfig:         APIConfig,
    terminationSignal: F[Unit],
    makeUsersDiscussionsConsumer: (
      ConsumerStream.Factory[F, DiscussionEvent],
      StreamRunner.FromConsumerStream[F, DiscussionEvent]
    ) => StreamRunner[F],
    makeNotificationsDiscussionsConsumer: (
      ConsumerStream.Factory[F, DiscussionEvent],
      StreamRunner.FromConsumerStream[F, DiscussionEvent]
    ) => StreamRunner[F],
    notificationTopic: NotificationTopic[F]
  )(
    registry:            CollectorRegistry,
    usersReads:          UsersReads[F],
    usersWrites:         UsersWrites[F],
    discussionsReads:    DiscussionsReads[F],
    discussionsWrites:   DiscussionsWrites[F],
    notificationsReads:  NotificationsReads[F],
    notificationsWrites: NotificationsWrites[F]
  ): F[Unit] = {
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
          subscriptionWrites = discussionsWrites.subscriptionWrites,
          notificationReads = notificationsReads.notificationReads,
          notificationWrites = notificationsWrites.notificationWrites,
          notificationTopic = notificationTopic
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
      ),
      // run Notifications projections on a separate thread
      notificationsWrites.runProjections.asResource.conditionally("Notifications' projections",
                                                                  appArguments.runNotificationsProjections
      ),
      // run Notifications' Discussions consumer on a separate thread
      makeNotificationsDiscussionsConsumer(discussionsReads.discussionEventConsumer,
                                           notificationsWrites.runDiscussionsConsumer
      ).asResource.conditionally("Notifications' Discussions consumer", appArguments.runNotificationsProjections)
    ).tupled >> logBeforeAfter[F]("Services initialized", "Received exit signal")
  }.use(_ => terminationSignal) // here we are blocking until e.g. user press Ctrl+C

  // kudos to Łukasz Byczyński
  private def awaitTerminationSignal[F[_]: Async]: F[Unit] = {
    def handleSignal(signalName: String): F[Unit] = Async[F].async_[Unit] { cb =>
      Signal.handle(new Signal(signalName), _ => cb(().asRight[Throwable]))
      ()
    }
    handleSignal("INT").race(handleSignal("TERM")).void
  }

  private def logBeforeAfter[F[_]: Functor](before: String, after: String)(using logger: Logger[F]) =
    Resource.make(Logger[F].info(before))(_ => logger.info(after))

  extension [F[_]: Monad: Logger](resource: Resource[F, Unit]) {

    def conditionally(name: String, condition: Boolean): Resource[F, Unit] =
      if (condition) {
        logBeforeAfter[F](s"Starting $name", s"$name shutdown completed") >>
          resource >>
          logBeforeAfter[F](s"$name start completed", s"Shutting down $name")
      } else Resource.eval(Logger[F].info(s"$name disabled - omitting"))
  }
}
