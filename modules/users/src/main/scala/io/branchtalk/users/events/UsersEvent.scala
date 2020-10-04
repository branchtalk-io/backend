package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._
import io.branchtalk.ADT
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait UsersEvent extends ADT
object UsersEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor)
  final case class ForUser(user: UserEvent) extends UsersEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor)
  final case class ForSession(session: SessionEvent) extends UsersEvent
}
