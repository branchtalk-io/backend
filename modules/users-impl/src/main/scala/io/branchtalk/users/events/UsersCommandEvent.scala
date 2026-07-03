package io.branchtalk.users.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.shared.model.{ FastEq, ShowPretty }

sealed trait UsersCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object UsersCommandEvent {
  final case class ForUser(user: UserCommandEvent) extends UsersCommandEvent
  final case class ForBan(ban: BanCommandEvent) extends UsersCommandEvent
}
