package io.branchtalk.users.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }
import io.branchtalk.users.model.{ Ban, User }

sealed trait BanCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object BanCommandEvent {

  final case class OrderBan(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    reason:        Ban.Reason,
    scope:         Ban.Scope,
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends BanCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class LiftBan(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    scope:         Ban.Scope,
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends BanCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
