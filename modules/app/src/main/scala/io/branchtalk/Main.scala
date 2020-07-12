package io.branchtalk

import cats.effect.ExitCode
import cats.implicits._
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.shared.infrastructure.DomainConfig
import monix.eval.{ Fiber, Task, TaskApp }
import pureconfig._

import scala.reflect.ClassTag

object Main extends TaskApp {

  def getEnv: Task[Map[String, String]] = Task(sys.env)

  def getConfig[A: ConfigReader: ClassTag](at: String): Task[A] =
    Task(ConfigSource.defaultApplication.at(at).loadOrThrow[A])

  def conditionalFiber(boolean: Boolean)(task: Task[Unit]): Task[Fiber[Unit]] =
    (if (boolean) task else Task.unit).start

  def initialize(arguments: Arguments)(
    discussionsReads:       DiscussionsReads[Task],
    discussionsWrites:      DiscussionsWrites[Task]
  ): Task[Unit] =
    for {
      apiFiber <- conditionalFiber(arguments.runApi) {
        val postServer = new PostServer[Task](discussionsWrites.postWrites)

        // TODO: use postServer.postRoutes (and other implementations) in initialized API

        Task.unit
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

  override def run(args: List[String]): Task[ExitCode] =
    (for {
      env <- getEnv
      arguments <- Arguments.parse(args, env)
      discussionsConfig <- getConfig[DomainConfig]("discussions")
      _ <- (
        DiscussionsModule.reads[Task](discussionsConfig),
        DiscussionsModule.writes[Task](discussionsConfig)
      ).tupled.use((initialize(arguments) _).tupled)
    } yield ExitCode.Success).onErrorHandle {
      case Arguments.ParsingError(help) =>
        println(help.toString())
        ExitCode.Error
      case error: Throwable =>
        error.printStackTrace()
        ExitCode.Error
    }
}
