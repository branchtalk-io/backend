package io.branchtalk.api

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import io.branchtalk.logging.{ MDC, RequestID }
import org.http4s.server.middleware.RequestId
import org.http4s._
import org.http4s.util.{ CaseInsensitiveString => CIString }

final class RequestIDOps[F[_]: Sync: MDC] {

  // reuses RequestId.httpRoutes but adds logging and MDC setup to it
  def httpRoutes(service: HttpRoutes[F]): HttpRoutes[F] = RequestId.httpRoutes(
    Kleisli { req: Request[F] =>
      for {
        _ <- req.headers
          .get(RequestIDOps.requestIdHeader)
          .map(_.value.pipe(RequestID(_)))
          .traverse(_.updateMDC[F].pipe(OptionT.liftF(_)))
        response <- service(req)
      } yield response
    }
  )
}
object RequestIDOps {

  private val requestIdHeader = CIString("X-Request-ID")

  def apply[F[_]: Sync: MDC]: RequestIDOps[F] = new RequestIDOps[F]
}
