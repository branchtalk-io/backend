package io.branchtalk.users.events

import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.branchtalk.users.model.{ Ban, User }
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait BanEvent extends ADT
object BanEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Banned(
    bannedUserID: ID[User],
    moderatorID:  Option[ID[User]],
    scope:        Ban.Scope,
    reason:       Ban.Reason,
    createdAt:    CreationTime
  ) extends BanEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Unbanned(
    bannedUserID: ID[User],
    moderatorID:  Option[ID[User]],
    scope:        Ban.Scope,
    modifiedAt:   ModificationTime
  ) extends BanEvent
}
