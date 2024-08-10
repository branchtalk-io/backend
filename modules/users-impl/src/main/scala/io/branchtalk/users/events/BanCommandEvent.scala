package io.branchtalk.users.events

import com.sksamuel.avro4s.*
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }
import io.branchtalk.users.model.{ Ban, User }

sealed trait BanCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object BanCommandEvent {

  final case class OrderBan(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    reason:        Ban.Reason,
    scope:         Ban.Scope,
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends BanCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class LiftBan(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    scope:         Ban.Scope,
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends BanCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
