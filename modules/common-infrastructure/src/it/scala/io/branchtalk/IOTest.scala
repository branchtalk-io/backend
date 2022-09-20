package io.branchtalk

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.branchtalk.logging.MDC
import io.branchtalk.logging.MDC.Ctx
import io.branchtalk.shared.model.CodePosition
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.{ AsExecution, Execution }
import org.specs2.matcher.Matchers._
import org.specs2.matcher.MustMatchers.theValue

import scala.concurrent.duration._

trait IOTest {

  val pass: MatchResult[Boolean] = true must beTrue

  // we don't rely on MDC in tests
  implicit protected val noopMDC: MDC[IO] = new MDC[IO] {
    override def ctx: IO[Ctx] = IO.pure(Map.empty)

    override def get(key: String): IO[Option[String]] = IO.pure(None)

    override def set(key: String, value: String): IO[Unit] = IO.unit
  }

  implicit class IOTestOps[T](private val io: IO[T]) {

    def eventually(retry: Int = 50, delay: FiniteDuration = 250.millis, timeout: FiniteDuration = 15.seconds)(implicit
      codePosition:       CodePosition
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

  implicit protected def ioAsTest[T: AsExecution]: AsExecution[IO[T]] = new AsExecution[IO[T]] {
    override def execute(t: => IO[T]): Execution = AsExecution[T].execute(t.unsafeRunSync())
  }
}
