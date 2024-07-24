package io.branchtalk.logging

import cats.effect.Sync
import io.branchtalk.shared.model.UUID
import neotype.*

type CorrelationID = CorrelationID.Type
object CorrelationID extends Newtype[String] {

  private val key = "correlation-id"

  def generate[F[_]: Sync](using UUID.Generator): F[CorrelationID] =
    UUID.create[F].map(_.show).map(unsafeMake)

  def getCurrent[F[_]: MDC]: F[Option[CorrelationID]] = unsafeMakeF[[A] =>> F[Option[A]]](MDC[F].get(key))

  def getCurrentOrGenerate[F[_]: Sync: MDC](using uuidGenerator: UUID.Generator): F[CorrelationID] =
    getCurrent[F].flatMap(_.fold(generate[F])(_.pure[F]))

  extension (cid: CorrelationID) def updateMDC[F[_]: MDC]: F[Unit] = MDC[F].set(key, cid.unwrap)
}
