package io.branchtalk

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.branchtalk.logging.MDC
import io.branchtalk.logging.MDC.Ctx
import io.branchtalk.shared.model.CodePosition
import org.specs2.execute.Result
import org.specs2.specification.core.{ AsExecution, Execution }
import org.specs2.matcher.MustMatchers.{ *, given }

import scala.concurrent.duration.*

trait IOTest {

  val pass: Result = true must beTrue

  // we don't rely on MDC in tests
  protected given noopMDC: MDC[IO] = new MDC[IO] {
    override def ctx: IO[Ctx] = IO.pure(Map.empty)

    override def get(key: String): IO[Option[String]] = IO.pure(None)

    override def set(key: String, value: String): IO[Unit] = IO.unit
  }

  extension [T](io: IO[T]) {

    def eventually(retry: Int = 50, delay: FiniteDuration = 250.millis, timeout: FiniteDuration = 15.seconds)(using
      codePosition: CodePosition
    ): IO[T] = {
      def withRetry(attemptsLeft: Int): PartialFunction[Throwable, IO[T]] = { case cause: Throwable =>
        if (attemptsLeft <= 0)
          IO.raiseError(new Exception(show"IO failed to succeed: exceeded retry $retry, from $codePosition", cause))
        else io.handleErrorWith(withRetry(attemptsLeft - 1)).delayBy(delay)
      }

      io.handleErrorWith(withRetry(retry)).timeout(timeout)
    }

    def assert(msg: String)(condition: T => Boolean): IO[T] =
      io.flatTap(current => IO(scala.Predef.assert(condition(current), msg)))
  }

  protected given ioAsTest[T: AsExecution]: AsExecution[IO[T]] = new AsExecution[IO[T]] {
    override def execute(t: => IO[T]): Execution = AsExecution[T].execute(t.unsafeRunSync())
  }
}
