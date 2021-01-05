package io.branchtalk

import cats.effect.ExitCode
import io.branchtalk.logging.MonixMDCAdapter
import monix.eval.{ Task, TaskApp }

object Main extends TaskApp {

  MonixMDCAdapter.configure()

  override def run(args: List[String]): Task[ExitCode] = Program.runApplication[Task](args)
}
