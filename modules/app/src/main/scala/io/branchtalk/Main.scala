package io.branchtalk

import cats.effect.ExitCode
import cats.implicits._
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import monix.eval.{ Task, TaskApp }
import pureconfig._

object Main extends TaskApp {

  def getEnv: Task[Map[String, String]] = Task(sys.env)

  def initialize(arguments: Arguments)(
    discussionsReads:       DiscussionsReads[Task],
    discussionsWrites:      DiscussionsWrites[Task]
  ): Task[Unit] =
    // TODO: initialize services as fibers
    Task.unit

  override def run(args: List[String]): Task[ExitCode] =
    for {
      env <- getEnv
      arguments <- Arguments.parse(args, env)
      discussionsConfig <- Task(ConfigSource.defaultApplication.at("discussions").loadOrThrow[DomainConfig])
      _ <- (DiscussionsModule.reads[Task](discussionsConfig), DiscussionsModule.writes[Task](discussionsConfig)).tupled
        .use((initialize(arguments) _).tupled)
    } yield ExitCode.Success
}
