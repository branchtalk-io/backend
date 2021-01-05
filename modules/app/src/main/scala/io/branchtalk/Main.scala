package io.branchtalk

import cats.effect.ExitCode
import io.branchtalk.logging.{ MDC, MonixMDC, MonixMDCAdapter }
import monix.eval.{ Task, TaskApp }

object Main extends TaskApp {

  MonixMDCAdapter.configure()

  implicit private val mdc: MDC[Task] = MonixMDC

  override def run(args: List[String]): Task[ExitCode] = Program.runApplication[Task](args)
}
