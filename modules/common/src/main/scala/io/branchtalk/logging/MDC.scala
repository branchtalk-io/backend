package io.branchtalk.logging

// Abstracts away how we perform MCD from what effect F we use.
trait MDC[F[_]] {

  def ctx: F[MDC.Ctx]

  def get(key: String): F[Option[String]]

  def set(key: String, value: String): F[Unit]
}
object MDC {

  type Ctx = Map[String, String]

  @inline def apply[F[_]](implicit mdc: MDC[F]): MDC[F] = mdc
}
