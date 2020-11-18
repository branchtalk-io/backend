package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.shared.model.{ FastEq, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait UsersCommandEvent extends ADT
object UsersCommandEvent {
  final case class ForUser(user: UserCommandEvent) extends UsersCommandEvent
}
