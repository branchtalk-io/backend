package io.branchtalk

import cats.effect.{ ContextShift, ExitCode, IO, IOApp, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.logging.MDC
import io.branchtalk.shared.model.CodePosition
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.{ AsExecution, Execution }
import org.specs2.matcher.Matchers._
import org.specs2.matcher.MustMatchers.theValue

import scala.concurrent.duration._

trait IOTest {

  private val logger = Logger(getClass)

  val pass: MatchResult[Boolean] = true must beTrue

  implicit protected val contextShift: ContextShift[IO] = IOTest.AllTheRightImplicits.contextShift
  implicit protected val timer:        Timer[IO]        = IOTest.AllTheRightImplicits.timer

  // IO doesn't have Local like Monix
  implicit protected val noopMDC: MDC[IO] = new MDC[IO] {
    override def enable[A](fa: IO[A]): IO[A] = fa

    override def get(key: String): IO[Option[String]] = IO.pure(None)

    override def set(key: String, value: String): IO[Unit] = IO.unit
  }

  private val ignoreErrorForLogging: PartialFunction[Throwable, Unit] = { case _: fs2.kafka.CommitTimeoutException => }

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
object IOTest {

  object AllTheRightImplicits extends IOApp {
    override def run(args: List[String]): IO[ExitCode] = ???
    override def contextShift: ContextShift[IO] = super.contextShift
    override def timer:        Timer[IO]        = super.timer
  }
}
