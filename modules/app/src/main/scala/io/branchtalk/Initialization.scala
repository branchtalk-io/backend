package io.branchtalk

import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, ExitCode, Resource, Sync, Timer }
import cats.effect.implicits._
import io.branchtalk.configs.{ AppConfig, Configuration }
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import io.branchtalk.shared.models.UUIDGenerator
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Initialization {

  // TODO: use some logger

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  def runApplication[F[_]: ConcurrentEffect: ContextShift: Timer](args: List[String]): F[ExitCode] =
    (for {
      env <- Configuration.getEnv[F]
      appConfig <- AppConfig.parse[F](args, env)
      discussionsConfig <- Configuration.readConfig[F, DomainConfig]("discussions")
      _ <- (
        DiscussionsModule.reads[F](discussionsConfig),
        DiscussionsModule.writes[F](discussionsConfig)
      ).tupled.use((runModules[F](appConfig, awaitTerminationSignal[F]) _).tupled)
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

  def runModules[F[_]: ConcurrentEffect: ContextShift: Timer](appConfig: AppConfig, terminationSignal: F[Unit])(
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): F[Unit] =
    Sync[F].delay(println("Initializing services")) >> // scalastyle:ignore
      (
        conditionalResource(appConfig.runAPI)(())(runApi[F](appConfig)(discussionsReads, discussionsWrites)),
        conditionalResource(appConfig.runDiscussionsProjections)(().pure[F])(discussionsWrites.runProjector)
      ).tupled.use {
        case (_, startDiscussions) =>
          for {
            discussionsFiber <- startDiscussions.start
            _ = println("Services initialized") // scalastyle:ignore
            _ <- terminationSignal // here we are blocking until e.g. user press Ctrl+D or Ctrl+C
            _ = println("Received exit signal") // scalastyle:ignore
            _ <- discussionsFiber.join
          } yield println("Shut down services") // scalastyle:ignore
      }

  private def runApi[F[_]: ConcurrentEffect: ContextShift: Timer](
    appConfig:        AppConfig
  )(discussionsReads: DiscussionsReads[F], discussionsWrites: DiscussionsWrites[F]): Resource[F, Unit] = {
    // TODO: refactor this
    // TODO: also swagger?
    val postServer = new PostServer[F](discussionsReads.postReads, discussionsWrites.postWrites)
    val httpApp    = postServer.postRoutes.orNotFound

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
