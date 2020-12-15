package io.branchtalk.users.events

import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.branchtalk.users.model.{ Ban, Channel, User }
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait BanEvent extends ADT
object BanEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class BannedInChannel(
    userID:    ID[User],
    channelID: ID[Channel],
    reason:    Ban.Reason
  ) extends BanEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class UnbannedInChannel(
    userID:    ID[User],
    channelID: ID[Channel]
  ) extends BanEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class BannedGlobally(
    userID: ID[User],
    reason: Ban.Reason
  ) extends BanEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class UnbannedGlobally(
    userID: ID[User]
  ) extends BanEvent
}
