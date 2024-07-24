package io.branchtalk.api

import cats.Order
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.shared.model.{ ShowPretty, UUID }
import neotype.*

enum Permission derives ShowPretty, DefaultJsCodec {
  case Administrate
  case IsOwner
  case ModerateUsers
  case ModerateChannel(channelID: ChannelID)
  case CanPublish(channelID: ChannelID)
}
object Permission {

  given Order[Permission] = {
    case (Administrate, Administrate)               => 0
    case (Administrate, _)                          => 1
    case (IsOwner, IsOwner)                         => 0
    case (IsOwner, _)                               => 1
    case (ModerateUsers, ModerateUsers)             => 0
    case (ModerateUsers, _)                         => 1
    case (ModerateChannel(c1), ModerateChannel(c2)) => Order[UUID].compare(c1.unwrap, c2.unwrap)
    case (ModerateChannel(_), _)                    => 1
    case (CanPublish(c1), CanPublish(c2))           => Order[UUID].compare(c1.unwrap, c2.unwrap)
    case (CanPublish(_), _)                         => -1
  }
}
