package io.branchtalk

import cats.effect.{ ExitCode, IO, IOApp }
import io.branchtalk.logging.{ IOGlobal, IOMDCAdapter }

object Main extends IOApp {

  // Runs Program using CE IO as the IO implementation.
  override def run(args: List[String]): IO[ExitCode] =
    IOMDCAdapter.configure.flatMap { mdc =>
      // Initializes local context propagation in IO, so that we would be able to use Mapped Diagnostic Context in logs.
      Program.runApplication[IO](args)(IOGlobal.configuredStatePropagation, mdc)
    }
}
