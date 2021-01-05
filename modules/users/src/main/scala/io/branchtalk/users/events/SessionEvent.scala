package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.branchtalk.users.model.{ Session, User }
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait SessionEvent extends ADT
object SessionEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class LoggedIn(
    id:            ID[Session],
    userID:        ID[User],
    expiresAt:     Session.ExpirationTime,
    correlationID: CorrelationID
  ) extends SessionEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class LoggedOut(
    id:            ID[Session],
    userID:        ID[User],
    correlationID: CorrelationID
  ) extends SessionEvent
}
