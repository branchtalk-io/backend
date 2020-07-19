package io.branchtalk

import cats.implicits._
import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, ExitCode, Fiber, Resource, Sync, Timer }
import cats.effect.implicits._
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import pureconfig.{ ConfigReader, ConfigSource }

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object Initialization {

  def getEnv[F[_]: Sync]: F[Map[String, String]] = Sync[F].delay(sys.env)

  def getConfig[F[_]: Sync, A: ConfigReader: ClassTag](at: String): F[A] =
    Sync[F].delay(ConfigSource.defaultApplication.at(at).loadOrThrow[A])

  def conditionalResource[F[_]: Concurrent, A](
    boolean: Boolean
  )(default: => A)(resource: Resource[F, A]): Resource[F, A] =
    if (boolean) resource else Resource.pure[F, A](default)

  def initialize[F[_]: ConcurrentEffect: ContextShift: Timer](arguments: Arguments)(
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): F[Unit] = {
    val api = conditionalResource(arguments.runApi)(()) {
      // TODO: refactor this
      // TODO: also swagger?
      val postServer = new PostServer[F](discussionsWrites.postWrites)
      val httpApp    = postServer.postRoutes.orNotFound

      val serverBuilder = BlazeServerBuilder[F](ExecutionContext.global)
        .bindHttp(port = arguments.port, host = arguments.host)
        .withHttpApp(httpApp)

      serverBuilder.resource.void
    }

    val discussionsProjections = conditionalResource(arguments.runDiscussionsProjections)(().pure[F]) {
      discussionsWrites.runProjector
    }

    (api, discussionsProjections).tupled.use {
      case (_, startDiscussions) =>
        for {
          discussionsFiber <- startDiscussions.start
          _ <- Async[F].never[Unit] // TODO: replace with some nice kill switch, e.g. Ctrl+C or Ctrl+D
          _ <- discussionsFiber.join
        } yield ()
    }
  }

  def run[F[_]: ConcurrentEffect: ContextShift: Timer](args: List[String]): F[ExitCode] =
    (for {
      env <- getEnv[F]
      arguments <- Arguments.parse[F](args, env)
      discussionsConfig <- getConfig[F, DomainConfig]("discussions")
      _ <- (
        DiscussionsModule.reads[F](discussionsConfig),
        DiscussionsModule.writes[F](discussionsConfig)
      ).tupled.use((initialize[F](arguments) _).tupled)
    } yield ExitCode.Success).handleError {
      case Arguments.ParsingError(help) =>
        println(help.toString())
        ExitCode.Error
      case error: Throwable =>
        error.printStackTrace()
        ExitCode.Error
    }
}
