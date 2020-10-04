package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._
import io.branchtalk.users.model.{ Session, User }
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait SessionEvent extends ADT
object SessionEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class LoggedIn(
    id:        ID[Session],
    userID:    ID[User],
    expiresAt: Session.ExpirationTime
  ) extends SessionEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class LoggedOut(
    id:     ID[Session],
    userID: ID[User]
  ) extends SessionEvent
}
