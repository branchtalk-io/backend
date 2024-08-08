package io.branchtalk.users.events

import com.sksamuel.avro4s.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait UsersEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object UsersEvent {

  final case class ForUser(
    user: UserEvent
  ) extends UsersEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class ForSession(session: SessionEvent) extends UsersEvent
      derives Decoder,
        Encoder,
        FastEq,
        ShowPretty,
        SchemaFor

  final case class ForBan(
    ban: BanEvent
  ) extends UsersEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
