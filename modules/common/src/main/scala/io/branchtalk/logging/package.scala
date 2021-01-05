package io.branchtalk

import cats.{ Order, Show }
import cats.effect.Sync
import io.branchtalk.shared.model.UUIDGenerator
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

package object logging {

  @newtype final case class CorrelationID(show: String) {

    def updateMDC[F[_]: MDC]: F[Unit] = MDC[F].set(CorrelationID.key, show)
  }
  object CorrelationID {

    private val key = "correlation-id"

    def generate[F[_]: Sync](implicit uuidGenerator: UUIDGenerator): F[CorrelationID] =
      uuidGenerator.create[F].map(_.toString).map(CorrelationID(_))

    def getCurrent[F[_]: MDC]: F[Option[CorrelationID]] = MDC[F].get(key).coerce

    def getCurrentOrGenerate[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator): F[CorrelationID] =
      getCurrent[F].flatMap(_.fold(generate[F])(_.pure[F]))

    implicit val show:  Show[CorrelationID]  = Show[String].coerce
    implicit val order: Order[CorrelationID] = Order[String].coerce
  }

  @newtype final case class RequestID(show: String) {

    def updateMDC[F[_]: MDC]: F[Unit] = MDC[F].set(RequestID.key, show)
  }
  object RequestID {

    private val key = "request-id"

    def generate[F[_]: Sync](implicit uuidGenerator: UUIDGenerator): F[RequestID] =
      uuidGenerator.create[F].map(_.toString).map(RequestID(_))

    def getCurrent[F[_]: MDC]: F[Option[RequestID]] = MDC[F].get(key).coerce

    def getCurrentOrGenerate[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator): F[RequestID] =
      getCurrent[F].flatMap(_.fold(generate[F])(_.pure[F]))

    implicit val show:  Show[RequestID]  = Show[String].coerce
    implicit val order: Order[RequestID] = Order[String].coerce
  }
}
