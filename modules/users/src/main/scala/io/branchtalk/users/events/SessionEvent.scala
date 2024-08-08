package io.branchtalk.users.events

import com.sksamuel.avro4s.*
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }
import io.branchtalk.users.model.{ Session, User }

sealed trait SessionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object SessionEvent {

  final case class LoggedIn(
    id:            ID[Session],
    userID:        ID[User],
    expiresAt:     Session.ExpirationTime,
    correlationID: CorrelationID
  ) extends SessionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class LoggedOut(
    id:            ID[Session],
    userID:        ID[User],
    correlationID: CorrelationID
  ) extends SessionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
