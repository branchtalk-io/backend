package io.branchtalk

import cats.effect.{ ContextShift, IO, Timer }

import scala.concurrent.ExecutionContext

trait IOTest {

  protected implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  protected implicit val timer:        Timer[IO]        = IO.timer(ExecutionContext.global)
}
