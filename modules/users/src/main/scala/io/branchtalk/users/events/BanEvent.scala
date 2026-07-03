package io.branchtalk.users.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }
import io.branchtalk.users.model.{ Ban, User }

sealed trait BanEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object BanEvent {

  final case class Banned(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    scope:         Ban.Scope,
    reason:        Ban.Reason,
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends BanEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Unbanned(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    scope:         Ban.Scope,
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends BanEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
