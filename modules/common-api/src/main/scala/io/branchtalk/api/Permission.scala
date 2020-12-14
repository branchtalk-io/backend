package io.branchtalk.api

import cats.Order
import io.branchtalk.ADT
import io.branchtalk.api.JsoniterSupport._
import io.branchtalk.shared.model.{ FastEq, ShowPretty, UUID }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty, JsCodec) sealed trait Permission extends ADT
object Permission {

  case object Administrate extends Permission
  case object IsOwner extends Permission
  case object ModerateUsers extends Permission
  final case class ModerateChannel(channelID: ChannelID) extends Permission
  final case class CanPublish(channelID: ChannelID) extends Permission

  implicit val order: Order[Permission] = {
    case (Administrate, Administrate)               => 0
    case (Administrate, _)                          => 1
    case (IsOwner, IsOwner)                         => 0
    case (IsOwner, _)                               => 1
    case (ModerateUsers, ModerateUsers)             => 0
    case (ModerateUsers, _)                         => 1
    case (ModerateChannel(c1), ModerateChannel(c2)) => Order[UUID].compare(c1.uuid, c2.uuid)
    case (ModerateChannel(_), _)                    => 1
    case (CanPublish(c1), CanPublish(c2))           => Order[UUID].compare(c1.uuid, c2.uuid)
    case (CanPublish(_), _)                         => -1
  }
}
