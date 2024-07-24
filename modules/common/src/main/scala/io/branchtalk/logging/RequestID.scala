package io.branchtalk.logging

import cats.effect.Sync
import io.branchtalk.shared.model.UUID
import neotype.*

type RequestID = RequestID.Type
object RequestID extends Newtype[String] {

  private val key = "request-id"

  def generate[F[_]: Sync](using UUID.Generator): F[RequestID] =
    UUID.create[F].map(_.show).map(RequestID(_))

  def getCurrent[F[_]: MDC]: F[Option[RequestID]] = unsafeMakeF[[A] =>> F[Option[A]]](MDC[F].get(key))

  def getCurrentOrGenerate[F[_]: Sync: MDC](using UUID.Generator): F[RequestID] =
    getCurrent[F].flatMap(_.fold(generate[F])(_.pure[F]))

  extension (rid: RequestID) def updateMDC[F[_]: MDC]: F[Unit] = MDC[F].set(key, rid.unwrap)
}
