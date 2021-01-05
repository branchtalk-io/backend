package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.branchtalk.users.model.{ Ban, User }
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait BanCommandEvent extends ADT
object BanCommandEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class OrderBan(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    reason:        Ban.Reason,
    scope:         Ban.Scope,
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends BanCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class LiftBan(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    scope:         Ban.Scope,
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends BanCommandEvent
}
