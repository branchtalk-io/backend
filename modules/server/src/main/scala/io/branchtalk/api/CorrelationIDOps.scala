package io.branchtalk.api

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.model.UUIDGenerator
import org.http4s._

final class CorrelationIDOps[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator) {

  def httpRoutes(service: HttpRoutes[F]): HttpRoutes[F] = Kleisli { req: Request[F] =>
    for {
      correlationID <- req.headers.get(CorrelationIDOps.correlationIDHeader) match {
        case None            => CorrelationID.generate[OptionT[F, *]]
        case Some(cidHeader) => CorrelationID(cidHeader.head.value).pure[OptionT[F, *]]
      }
      _ <- correlationID.updateMDC[F].pipe(OptionT.liftF(_))
      reqWithID = req.putHeaders(Header.Raw(CorrelationIDOps.correlationIDHeader, correlationID.show))
      response <- service(reqWithID)
    } yield response
  }
}
object CorrelationIDOps {

  private val correlationIDHeader = org.typelevel.ci.CIString("X-Correlation-ID")

  def apply[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator): CorrelationIDOps[F] = new CorrelationIDOps[F]
}
