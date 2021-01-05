package io.branchtalk

import cats.Show
import cats.effect.Sync
import io.branchtalk.shared.model.UUIDGenerator
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

package object logging {

  @newtype final case class CorrelationID(show: String) {

    def updateMDC[F[_]: MDC]: F[Unit] = implicitly[MDC[F]].set("correletion-id", show)
  }
  object CorrelationID {

    def generate[F[_]: Sync](implicit uuidGenerator: UUIDGenerator): F[CorrelationID] =
      uuidGenerator.create[F].map(_.toString).map(CorrelationID(_))

    implicit val show: Show[CorrelationID] = Show[String].coerce
  }
}
