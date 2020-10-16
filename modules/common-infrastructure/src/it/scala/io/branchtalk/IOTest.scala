package io.branchtalk

import cats.effect.{ ContextShift, IO, Timer }
import org.specs2.specification.core.AsExecution

import scala.concurrent.duration._

trait IOTest {

  protected implicit val contextShift: ContextShift[IO] =
    IO.contextShift(scala.concurrent.ExecutionContext.fromExecutor(null))
  protected implicit val timer: Timer[IO] =
    IO.timer(scala.concurrent.ExecutionContext.fromExecutor(null))

  implicit class IOTestOps[T](private val io: IO[T]) {

    def eventually(retry: Int = 90, delay: FiniteDuration = 100.millis, timeout: FiniteDuration = 15.seconds): IO[T] = {
      val timeouting = IO.sleep(timeout) >> IO.raiseError(new Exception(s"IO failed: exceeded timeout $timeout"))

      def withRetry(attemptsLeft: Int): PartialFunction[Throwable, IO[T]] = {
        case cause: Throwable =>
          if (attemptsLeft <= 0) IO.raiseError(new Exception(s"IO failed to succeed: exceeded retry $retry", cause))
          else io.handleErrorWith(withRetry(attemptsLeft - 1)).delayBy(delay)
      }

      IO.race(io.handleErrorWith(withRetry(retry)), timeouting).map {
        case Left(value) => value
        case Right(_)    => ??? // impossible
      }
    }
  }

  protected implicit def ioAsTest[T: AsExecution]: AsExecution[IO[T]] = new AsExecution[IO[T]] {
    override def execute(t: => IO[T]) = AsExecution[T].execute(t.unsafeRunSync())
  }
}
