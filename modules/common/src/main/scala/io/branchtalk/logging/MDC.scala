package io.branchtalk.logging

// Abstracts away how we perform MCD from what effect F we use.
trait MDC[F[_]] {

  def enable[A](fa: F[A]): F[A]

  def get(key: String): F[Option[String]]

  def set(key: String, value: String): F[Unit]
}
object MDC {

  @inline def apply[F[_]](implicit mdc: MDC[F]): MDC[F] = mdc
}
