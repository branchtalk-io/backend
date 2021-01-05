package io.branchtalk.api

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.model.{ Logger, UUIDGenerator }
import org.http4s._
import org.http4s.util.{ CaseInsensitiveString => CIString }

final class CorrelationIDOps[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator) {

  private val logger = Logger.getLogger[OptionT[F, *]]

  def httpRoutes(service: HttpRoutes[F]): HttpRoutes[F] = Kleisli { req: Request[F] =>
    for {
      correlationID <- req.headers.get(CorrelationIDOps.correlationIDHeader) match {
        case None            => CorrelationID.generate[OptionT[F, *]]
        case Some(cidHeader) => CorrelationID(cidHeader.value).pure[OptionT[F, *]]
      }
      _ <- correlationID.updateMDC[F].pipe(OptionT.liftF(_))
      _ <- logger.info(show"Start request with CoordinationID=$correlationID")
      reqWithID = req.putHeaders(Header.Raw(CorrelationIDOps.correlationIDHeader, correlationID.show))
      response <- service(reqWithID)
    } yield response
  }
}
object CorrelationIDOps {

  private val correlationIDHeader = CIString("X-Correlation-ID")

  def apply[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator): CorrelationIDOps[F] = new CorrelationIDOps[F]
}
