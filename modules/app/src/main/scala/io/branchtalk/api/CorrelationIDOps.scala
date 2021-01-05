package io.branchtalk.api

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.model.{ Logger, UUIDGenerator }
import org.http4s._
import org.http4s.util.CaseInsensitiveString

final class CorrelationIDOps[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator) {

  private val logger = Logger.getLogger[F]

  def configure(service: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      val correlationID = req.headers.get(CaseInsensitiveString(CorrelationIDOps.CorrelationIDHeader)) match {
        case None            => CorrelationID.generate[F]
        case Some(cidHeader) => CorrelationID(cidHeader.value).pure[F]
      }
      correlationID
        .flatMap(_.updateMDC[F])
        .flatTap(id => logger.info(show"Start request with CoordinationID=$id"))
        .pipe(OptionT.liftF(_)) >> service(req)
    }
}
object CorrelationIDOps {

  private val CorrelationIDHeader = "X-Correlation-ID"

  def apply[F[_]: Sync: MDC](implicit uuidGenerator: UUIDGenerator): CorrelationIDOps[F] =
    new CorrelationIDOps[F]
}
