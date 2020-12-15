package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.branchtalk.users.model.{ Ban, User }
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait BanCommandEvent extends ADT
object BanCommandEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class BanInChannel(
    bannedUserID: ID[User],
    reason:       Ban.Reason,
    scope:        Ban.Scope
  ) extends BanCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class LiftBanInChannel(
    bannedUserID: ID[User],
    scope:        Ban.Scope
  ) extends BanCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class BanGlobally(
    bannedUserID: ID[User],
    reason:       Ban.Reason,
    scope:        Ban.Scope
  )

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class LiftBanGlobally(
    bannedUserID: ID[User],
    scope:        Ban.Scope
  ) extends BanCommandEvent
}
