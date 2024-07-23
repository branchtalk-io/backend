package io.branchtalk.logging

import cats.effect.Sync
import io.branchtalk.shared.model.UUIDGenerator
import neotype.*

type CorrelationID = CorrelationID.Type
object CorrelationID extends Newtype[String] {

  private val key = "correlation-id"

  def generate[F[_]: Sync](using uuidGenerator: UUIDGenerator): F[CorrelationID] =
    uuidGenerator.create[F].map(_.toString).map(unsafeMake)

  def getCurrent[F[_]: MDC]: F[Option[CorrelationID]] = MDC[F].get(key).asInstanceOf[F[Option[CorrelationID]]]

  def getCurrentOrGenerate[F[_]: Sync: MDC](using uuidGenerator: UUIDGenerator): F[CorrelationID] =
    getCurrent[F].flatMap(_.fold(generate[F])(_.pure[F]))

  extension (cid: CorrelationID) def updateMDC[F[_]: MDC]: F[Unit] = MDC[F].set(key, cid.unwrap)
}
