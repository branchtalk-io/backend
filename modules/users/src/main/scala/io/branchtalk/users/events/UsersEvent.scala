package io.branchtalk.users.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait UsersEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object UsersEvent {

  final case class ForUser(
    user: UserEvent
  ) extends UsersEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class ForSession(session: SessionEvent) extends UsersEvent
      derives AvroEncoder,
        AvroDecoder,
        FastEq,
        ShowPretty

  final case class ForBan(
    ban: BanEvent
  ) extends UsersEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
