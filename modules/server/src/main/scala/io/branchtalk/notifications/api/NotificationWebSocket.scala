package io.branchtalk.notifications.api

import cats.effect.Async
import com.github.plokhotnyuk.jsoniter_scala.core.*
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.auth.AuthServices
import io.branchtalk.api.{ Authentication, AuthenticationSupport }
import io.branchtalk.notifications.api.NotificationModels.APINotification
import io.branchtalk.notifications.model.User as NotifUser
import io.branchtalk.notifications.writes.NotificationTopic
import io.branchtalk.shared.model.ID
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax.header.*
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

// Plain http4s websocket route for streaming notifications to authenticated users in real time.
// The integration point must provide a WebSocketBuilder2[F] (from withHttpWebSocketApp on the server builder).
final class NotificationWebSocket[F[_]: Async](
  authServices:      AuthServices[F],
  notificationTopic: NotificationTopic[F]
) extends Http4sDsl[F] {

  def routes(wsb: WebSocketBuilder2[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root / "notifications" / "ws" =>
      // Extract auth from query parameter or Authorization header
      val authOpt: Option[String] =
        req.params.get("token").orElse(req.headers.get[headers.Authorization].map(_.value))
      authOpt match {
        case None =>
          Forbidden("Authentication required")
        case Some(authValue) =>
          val decoded = AuthenticationSupport.authHeaderMapping.decode(authValue)
          decoded match {
            case sttp.tapir.DecodeResult.Value(auth) =>
              Async[F].handleErrorWith(
                authServices.authenticateUser(auth).flatMap { case (user, _) =>
                  val recipientID = ID[NotifUser](user.id.unwrap)
                  val send = notificationTopic.subscribe(recipientID).map { notification =>
                    val apiNotif = APINotification.fromDomain(notification)
                    val json     = writeToString(apiNotif)
                    WebSocketFrame.Text(json)
                  }
                  val receive: fs2.Pipe[F, WebSocketFrame, Unit] = _.drain
                  wsb.build(send, receive)
                }
              )(_ => Forbidden("Invalid credentials"))
            case _ =>
              Forbidden("Invalid authentication format")
          }
      }
  }
}
