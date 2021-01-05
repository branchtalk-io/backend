package io.branchtalk.logging

trait MDC[F[_]] {

  def enable[A](fa: F[A]): F[A]

  def get(key: String): F[Option[String]]

  def set(key: String, value: String): F[Unit]
}
object MDC {

  def apply[F[_]](implicit mdc: MDC[F]): MDC[F] = mdc
}
