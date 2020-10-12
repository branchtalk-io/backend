package io.branchtalk

import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, ExitCode, Resource, Sync, Timer }
import cats.effect.implicits._
import io.branchtalk.configs.{ APIConfig, APIPart, AppConfig, Configuration }
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.users.{ UsersModule, UsersReads, UsersWrites }
import io.branchtalk.users.services.AuthServicesImpl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Program {

  // TODO: use some logger

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
    Sync[F].delay(println("Initializing services")) >> // scalastyle:ignore
      (
        conditionalResource(appConfig.runAPI)(())(
          runApi[F](appConfig, apiConfig)(usersReads, usersWrites, discussionsReads, discussionsWrites)
        ),
        conditionalResource(appConfig.runUsersProjections)(().pure[F])(usersWrites.runProjector),
        conditionalResource(appConfig.runDiscussionsProjections)(().pure[F])(discussionsWrites.runProjector)
      ).tupled.use {
        case (_, _, startDiscussions) =>
          for {
            discussionsFiber <- startDiscussions.start
            _ = println("Services initialized") // scalastyle:ignore
            _ <- terminationSignal // here we are blocking until e.g. user press Ctrl+D or Ctrl+C
            _ = println("Received exit signal") // scalastyle:ignore
            _ <- discussionsFiber.join
          } yield println("Shut down services") // scalastyle:ignore
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
    val authServices = new AuthServicesImpl[F](usersReads.userReads, usersReads.sessionReads)
    // TODO: refactor this
    // TODO: also swagger?
    val postServer = new PostServer[F](
      authServices,
      discussionsReads.postReads,
      discussionsWrites.postWrites,
      discussionsReads.subscriptionReads,
      apiConfig.safePagination(APIPart.Posts)
    )
    val httpApp = postServer.postRoutes.orNotFound

    val serverBuilder = BlazeServerBuilder[F](ExecutionContext.global) // TODO: configure some thread pool for HTTP
      .bindHttp(port = appConfig.port, host = appConfig.host)
      .withHttpApp(httpApp)

    serverBuilder.resource.void
  }

  // TODO: replace with some nice kill switch, e.g. Ctrl+C or Ctrl+D
  private def awaitTerminationSignal[F[_]: Async]: F[Unit] = Async[F].never[Unit]

  private def conditionalResource[F[_]: Concurrent, A](
    boolean: Boolean
  )(default: => A)(resource: Resource[F, A]): Resource[F, A] =
    if (boolean) resource else Resource.pure[F, A](default)
}
