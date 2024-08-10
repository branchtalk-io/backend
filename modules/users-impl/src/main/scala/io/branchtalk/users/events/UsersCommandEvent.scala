package io.branchtalk.users.events

import com.sksamuel.avro4s.*
import io.branchtalk.shared.model.{ FastEq, ShowPretty }

sealed trait UsersCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object UsersCommandEvent {
  final case class ForUser(user: UserCommandEvent) extends UsersCommandEvent
  final case class ForBan(ban: BanCommandEvent) extends UsersCommandEvent
}
