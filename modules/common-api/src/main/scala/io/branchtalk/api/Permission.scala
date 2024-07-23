package io.branchtalk.api

import cats.Order
import io.branchtalk.ADT
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.shared.model.{ ShowPretty, UUID }

enum Permission derives ShowPretty, JsCodec {
  case Administrate
  case IsOwner
  case ModerateUsers
  case ModerateChannel(channelID: ChannelID)
  case CanPublish(channelID: ChannelID)
}
@SuppressWarnings(Array("org.wartremover.warts.All")) // macros
object Permission {

  given Order[Permission] = {
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
