package io.branchtalk.users.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }
import io.branchtalk.users.model.{ Session, User }

sealed trait SessionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object SessionEvent {

  final case class LoggedIn(
    id:            ID[Session],
    userID:        ID[User],
    expiresAt:     Session.ExpirationTime,
    ipAddress:     Option[Session.IpAddress],
    userAgent:     Option[Session.UserAgent],
    correlationID: CorrelationID
  ) extends SessionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class LoggedOut(
    id:            ID[Session],
    userID:        ID[User],
    correlationID: CorrelationID
  ) extends SessionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
