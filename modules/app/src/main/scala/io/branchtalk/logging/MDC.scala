package io.branchtalk.logging

import monix.eval.Task

trait MDC[F[_]] {

  def set(key: String, value: String): F[Unit]
}
object MDC {

  implicit val monix: MDC[Task] = (key: String, value: String) =>
    Task(org.log4s.MDC(key) = value).executeWithOptions(_.enableLocalContextPropagation)
}
