package io.branchtalk.logging

import cats.effect.Sync
import io.branchtalk.shared.model.UUIDGenerator
import neotype.*

type RequestID = RequestID.Type
object RequestID extends Newtype[String] {

  private val key = "request-id"

  def generate[F[_]: Sync](implicit uuidGenerator: UUIDGenerator): F[RequestID] =
    uuidGenerator.create[F].map(_.toString).map(RequestID(_))

  def getCurrent[F[_]: MDC]: F[Option[RequestID]] = MDC[F].get(key).asInstanceOf[F[Option[RequestID]]]

  def getCurrentOrGenerate[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator): F[RequestID] =
    getCurrent[F].flatMap(_.fold(generate[F])(_.pure[F]))

  extension (rid: RequestID) def updateMDC[F[_]: MDC]: F[Unit] = MDC[F].set(key, rid.unwrap)
}
