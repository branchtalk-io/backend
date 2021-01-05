package io.branchtalk.logging

import monix.eval.Task

trait MDC[F[_]] {

  def get(key: String): F[Option[String]]

  def set(key: String, value: String): F[Unit]
}
object MDC {

  def apply[F[_]](implicit mdc: MDC[F]): MDC[F] = mdc

  implicit val monix: MDC[Task] = new MDC[Task] {
    override def get(key: String): Task[Option[String]] = Task(org.log4s.MDC.get(key))

    override def set(key: String, value: String): Task[Unit] =
      Task(org.log4s.MDC(key) = value).executeWithOptions(_.enableLocalContextPropagation)
  }
}
