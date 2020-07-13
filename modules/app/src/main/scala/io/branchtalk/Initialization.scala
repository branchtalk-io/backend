package io.branchtalk

import cats.implicits._
import cats.effect.{ Concurrent, ConcurrentEffect, ContextShift, ExitCode, Fiber, Sync, Timer }
import cats.effect.implicits._
import cats.Applicative
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import pureconfig.{ ConfigReader, ConfigSource }

import scala.reflect.ClassTag

object Initialization {

  def getEnv[F[_]: Sync]: F[Map[String, String]] = Sync[F].delay(sys.env)

  def getConfig[F[_]: Sync, A: ConfigReader: ClassTag](at: String): F[A] =
    Sync[F].delay(ConfigSource.defaultApplication.at(at).loadOrThrow[A])

  def conditionalFiber[F[_]: Concurrent](boolean: Boolean)(task: F[Unit]): F[Fiber[F, Unit]] =
    Concurrent[F].start((if (boolean) task else Applicative[F].unit))

  def initialize[F[_]: Concurrent: ContextShift](arguments: Arguments)(
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): F[Unit] =
    for {
      apiFiber <- conditionalFiber(arguments.runApi) {
        val postServer = new PostServer[F](discussionsWrites.postWrites)

        // TODO: use postServer.postRoutes (and other implementations) in initialized API

        Sync[F].unit
      }
      discussionsFiber <- conditionalFiber(arguments.runDiscussionsProjections) {
        discussionsWrites.runProjector.flatMap {
          case (start, killSwitch) =>
            // TODO: use killswitch for some graceful shutdown or sth
            start
        }.void
      }
      _ <- apiFiber.join
      _ <- discussionsFiber.join
    } yield ()

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
