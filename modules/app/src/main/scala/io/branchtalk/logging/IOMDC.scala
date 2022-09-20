package io.branchtalk.logging

import cats.effect.{ IO, IOLocal }

final class IOMDC(local: IOLocal[MDC.Ctx]) extends MDC[IO] {

  override def ctx: IO[MDC.Ctx] = local.get

  override def get(key: String): IO[Option[String]] = ctx.map(_.get(key))

  override def set(key: String, value: String): IO[Unit] = local.update(_.updated(key, value))
}
