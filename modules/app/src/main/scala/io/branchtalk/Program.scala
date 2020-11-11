package io.branchtalk

import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, ExitCode, Resource, Sync, Timer }
import cats.effect.implicits._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.AppServer
import io.branchtalk.configs.{ APIConfig, AppConfig, Configuration }
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.users.{ UsersModule, UsersReads, UsersWrites }

object Program {

  private val logger = Logger(getClass)

  implicit protected val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  def runApplication[F[_]: ConcurrentEffect: ContextShift: Timer](args: List[String]): F[ExitCode] =
    (for {
      env <- Configuration.getEnv[F]
      appConfig <- AppConfig.parse[F](args, env)
      apiConfig <- Configuration.readConfig[F, APIConfig]("api")
      usersConfig <- Configuration.readConfig[F, DomainConfig]("users")
      discussionsConfig <- Configuration.readConfig[F, DomainConfig]("discussions")
      _ <- (
        UsersModule.reads[F](usersConfig),
        UsersModule.writes[F](discussionsConfig),
        DiscussionsModule.reads[F](discussionsConfig),
        DiscussionsModule.writes[F](discussionsConfig)
      ).tupled.use((runModules[F](appConfig, apiConfig, awaitTerminationSignal[F]) _).tupled)
    } yield ExitCode.Success).handleError {
      case noConfig @ AppConfig.NoConfig(help) =>
        if (help.errors.nonEmpty) noConfig.printError()
        else noConfig.printHelp(ConfigFactory.defaultApplication())
      case error: Throwable =>
        error.printStackTrace()
        ExitCode.Error
    }

  def runModules[F[_]: ConcurrentEffect: ContextShift: Timer](
    appConfig:         AppConfig,
    apiConfig:         APIConfig,
    terminationSignal: F[Unit]
  )(
    usersReads:        UsersReads[F],
    usersWrites:       UsersWrites[F],
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): F[Unit] =
    Sync[F].delay(logger.info("Initializing services")) >> (
      conditionalResource(appConfig.runAPI)(())(
        AppServer
          .asResource(
            appConfig = appConfig,
            apiConfig = apiConfig,
            usersReads = usersReads,
            usersWrites = usersWrites,
            discussionsReads = discussionsReads,
            discussionsWrites = discussionsWrites
          )
          .void
      ),
      conditionalResource(appConfig.runUsersProjections)(().pure[F])(usersWrites.runProjector),
      conditionalResource(appConfig.runDiscussionsProjections)(().pure[F])(discussionsWrites.runProjector)
    ).tupled.use { case (_, _, startDiscussions) =>
      for {
        discussionsFiber <- startDiscussions.start
        _ <- Sync[F].delay(logger.info("Services initialized"))
        _ <- terminationSignal // here we are blocking until e.g. user press Ctrl+D or Ctrl+C
        _ <- Sync[F].delay(logger.info("Received exit signal"))
        _ <- discussionsFiber.join
        _ <- Sync[F].delay(logger.info("Shut down services"))
      } yield ()
    }

  private def awaitTerminationSignal[F[_]: Async]: F[Unit] = Async[F].never[Unit]

  private def conditionalResource[F[_]: Concurrent, A](
    boolean: Boolean
  )(default: => A)(resource: Resource[F, A]): Resource[F, A] =
    if (boolean) resource else Resource.pure[F, A](default)
}
