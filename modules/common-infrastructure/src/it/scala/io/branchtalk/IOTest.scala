package io.branchtalk

import cats.effect.{ ContextShift, IO, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.shared.model.CodePosition
import org.specs2.specification.core.{ AsExecution, Execution }

import scala.concurrent.duration._

trait IOTest {

  private val logger = Logger(getClass)

  implicit protected val contextShift: ContextShift[IO] =
    IO.contextShift(scala.concurrent.ExecutionContext.fromExecutor(null))
  implicit protected val timer: Timer[IO] =
    IO.timer(scala.concurrent.ExecutionContext.fromExecutor(null))

  private val ignoreErrorForLogging: PartialFunction[Throwable, Unit] = { case _: fs2.kafka.CommitTimeoutException =>
  }

  implicit class IOTestOps[T](private val io: IO[T]) {

    def eventually(retry: Int = 50, delay: FiniteDuration = 250.millis, timeout: FiniteDuration = 15.seconds)(implicit
      codePosition:       CodePosition
    ): IO[T] = {
      def withRetry(attemptsLeft: Int): PartialFunction[Throwable, IO[T]] = { case cause: Throwable =>
        if (attemptsLeft <= 0)
          IO.raiseError(new Exception(s"IO failed to succeed: exceeded retry $retry, from ${codePosition.show}", cause))
        else io.handleErrorWith(withRetry(attemptsLeft - 1)).delayBy(delay)
      }

      io.handleErrorWith(withRetry(retry)).timeout(timeout)
    }

    def logError(msg: String): IO[T] = io.handleErrorWith { error =>
      (if (ignoreErrorForLogging.isDefinedAt(error)) IO.unit else IO(logger.error(msg, error))) >> IO.raiseError(error)
    }

    def assert(msg: String)(condition: T => Boolean): IO[T] =
      io.flatTap(current => IO(scala.Predef.assert(condition(current), msg)))
  }

  implicit protected def ioAsTest[T: AsExecution]: AsExecution[IO[T]] = new AsExecution[IO[T]] {
    override def execute(t: => IO[T]): Execution = AsExecution[T].execute(t.unsafeRunSync())
  }
}
