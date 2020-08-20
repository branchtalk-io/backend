package io.branchtalk

import cats.effect.{ ContextShift, IO, Timer }
import org.specs2.specification.core.AsExecution

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait IOTest {

  protected implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  protected implicit val timer:        Timer[IO]        = IO.timer(ExecutionContext.global)

  implicit class IOTestOps[T](private val io: IO[T]) {

    def eventually(retries: Int = 40, duration: FiniteDuration = 5.seconds): IO[T] = {
      val timeout = IO.sleep(duration) >> IO.raiseError(
        new Exception(s"IO failed to succeed: exceeded duration $duration")
      )

      def retry(attemptsLeft: Int): PartialFunction[Throwable, IO[T]] = {
        case cause: Throwable =>
          if (attemptsLeft <= 0) IO.raiseError(new Exception(s"IO failed to succeed: exceeded retries $retries", cause))
          else io.handleErrorWith(retry(attemptsLeft - 1))
      }

      IO.race(io.handleErrorWith(retry(retries)), timeout).map {
        case Left(value) => value
        case Right(_)    => ??? // impossible
      }
    }
  }

  protected implicit def ioAsTest[T: AsExecution]: AsExecution[IO[T]] = new AsExecution[IO[T]] {
    override def execute(t: => IO[T]) = AsExecution[T].execute(t.unsafeRunSync())
  }
}
