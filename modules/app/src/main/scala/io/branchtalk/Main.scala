package io.branchtalk

import cats.effect.{ ExitCode, IO, IOApp }
import io.branchtalk.logging.IOMDCAdapter

object Main extends IOApp {

  // Runs Program using CE IO as the IO implementation.
  override def run(args: List[String]): IO[ExitCode] =
    IOMDCAdapter.configure.flatMap { mdc =>
      // cats-effect propagates the MDC IOLocal to log statements itself (see -Dcats.effect.ioLocalPropagation=true in
      // the build), so the plain IO Async instance is enough here.
      Program.runApplication[IO](args)(using IO.asyncForIO, mdc)
    }
}
