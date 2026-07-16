package io.branchtalk.users.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.shared.model.{ FastEq, ShowPretty }
// Brings branchtalk's base-type codecs (OffsetDateTime as String, ...) into scope: deriving this sum inline re-derives
// the wrapped UserCommandEvent/BanCommandEvent, so their base-type fields must resolve to branchtalk's codecs here too
// (a bare shared.model.* import does not import givens under -source 3.3-migration).
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait UsersCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object UsersCommandEvent {
  final case class ForUser(user: UserCommandEvent) extends UsersCommandEvent
  final case class ForBan(ban: BanCommandEvent) extends UsersCommandEvent
}
