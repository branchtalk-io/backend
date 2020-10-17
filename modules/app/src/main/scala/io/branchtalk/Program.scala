package io.branchtalk

import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, ExitCode, Resource, Sync, Timer }
import cats.effect.implicits._
import com.softwaremill.macwire.wire
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.{ AppServer, OpenAPIServer }
import io.branchtalk.configs.{ APIConfig, APIPart, AppConfig, Configuration, PaginationConfig }
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.users.{ UsersModule, UsersReads, UsersWrites }
import io.branchtalk.users.api.UserServer
import io.branchtalk.users.services.{ AuthServices, AuthServicesImpl }
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Program {

  private val logger = Logger(getClass)

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

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
      case AppConfig.NoConfig(help) =>
        // scalastyle:off regex
        if (help.errors.nonEmpty) {
          println("Invalid arguments:")
          println(help.errors.map("  " + _).intercalate("\n"))
          ExitCode.Error
        } else {
          println(help.toString())
          ExitCode.Success
        }
      // scalastyle:on regex
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
        runApi[F](appConfig, apiConfig)(usersReads, usersWrites, discussionsReads, discussionsWrites)
      ),
      conditionalResource(appConfig.runUsersProjections)(().pure[F])(usersWrites.runProjector),
      conditionalResource(appConfig.runDiscussionsProjections)(().pure[F])(discussionsWrites.runProjector)
    ).tupled.use {
      case (_, _, startDiscussions) =>
        for {
          discussionsFiber <- startDiscussions.start
          _ <- Sync[F].delay(logger.info("Services initialized"))
          _ <- terminationSignal // here we are blocking until e.g. user press Ctrl+D or Ctrl+C
          _ <- Sync[F].delay(logger.info("Received exit signal"))
          _ <- discussionsFiber.join
          _ <- Sync[F].delay(logger.info("Shut down services"))
        } yield ()
    }

  private def runApi[F[_]: ConcurrentEffect: ContextShift: Timer](
    appConfig: AppConfig,
    apiConfig: APIConfig
  )(
    usersReads:        UsersReads[F],
    usersWrites:       UsersWrites[F],
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): Resource[F, Unit] = {
    // this is kind of silly...
    import usersReads.{ sessionReads, userReads }
    import usersWrites.{ sessionWrites, userWrites }
    import discussionsReads.{ channelReads, commentReads, postReads, subscriptionReads }
    import discussionsWrites.{ channelWrites, commentWrites, postWrites, subscriptionWrites }

    val authServices: AuthServices[F] = wire[AuthServicesImpl[F]]

    val usersServer: UserServer[F] = {
      val paginationConfig: PaginationConfig = apiConfig.safePagination(APIPart.Users)
      wire[UserServer[F]]
    }
    val postServer: PostServer[F] = {
      val paginationConfig: PaginationConfig = apiConfig.safePagination(APIPart.Posts)
      wire[PostServer[F]]
    }
    val openAPIServer: OpenAPIServer[F] = {
      import apiConfig.info
      wire[OpenAPIServer[F]]
    }

    val appServer = wire[AppServer[F]]

    BlazeServerBuilder[F](ExecutionContext.global)
      .bindHttp(port = appConfig.port, host = appConfig.host)
      .withHttpApp(appServer.routes)
      .resource
      .void
  }

  private def awaitTerminationSignal[F[_]: Async]: F[Unit] = Async[F].never[Unit]

  private def conditionalResource[F[_]: Concurrent, A](
    boolean: Boolean
  )(default: => A)(resource: Resource[F, A]): Resource[F, A] =
    if (boolean) resource else Resource.pure[F, A](default)
}
