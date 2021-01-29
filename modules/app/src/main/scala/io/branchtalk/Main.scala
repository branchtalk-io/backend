package io.branchtalk

import cats.effect.ExitCode
import io.branchtalk.logging.{ MDC, MonixMDC, MonixMDCAdapter }
import monix.eval.{ Task, TaskApp }

object Main extends TaskApp {

  // Initializes local context propagation in Monix, so that we would be able to use Mapped Diagnostic Context in logs.
  MonixMDCAdapter.configure()

  // Defines MDC handing for Task.
  implicit private val mdc: MDC[Task] = MonixMDC

  // Runs Program using Task as the IO implementation.
  override def run(args: List[String]): Task[ExitCode] = Program.runApplication[Task](args)
}
