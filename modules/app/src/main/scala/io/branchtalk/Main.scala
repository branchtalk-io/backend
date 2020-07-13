package io.branchtalk

import cats.effect.ExitCode
import monix.eval.{ Task, TaskApp }

object Main extends TaskApp {

  override def run(args: List[String]): Task[ExitCode] = Initialization.run[Task](args)
}
