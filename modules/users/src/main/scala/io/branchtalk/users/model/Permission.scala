package io.branchtalk.users.model

import cats.Order
import io.branchtalk.shared.model.*

enum Permission derives ShowPretty {
  case Administrate
  case IsUser(userID: ID[User])
  case ModerateUsers
  case ModerateChannel(channelID: ID[Channel])
  case CanPublish(channelID: ID[Channel])
}
object Permission {

  enum Update derives FastEq, ShowPretty {
    case Add(permission: Permission)
    case Remove(permission: Permission)
  }

  given Order[Permission] = {
    case (Administrate, Administrate)               => 0
    case (Administrate, _)                          => 1
    case (IsUser(u1), IsUser(u2))                   => Order[ID[User]].compare(u1, u2)
    case (IsUser(_), _)                             => 1
    case (ModerateUsers, ModerateUsers)             => 0
    case (ModerateUsers, _)                         => 1
    case (ModerateChannel(c1), ModerateChannel(c2)) => Order[ID[Channel]].compare(c1, c2)
    case (ModerateChannel(_), _)                    => 1
    case (CanPublish(c1), CanPublish(c2))           => Order[ID[Channel]].compare(c1, c2)
    case (CanPublish(_), _)                         => -1
  }
}
