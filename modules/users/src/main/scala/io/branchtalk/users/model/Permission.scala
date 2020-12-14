package io.branchtalk.users.model

import cats.Order
import io.branchtalk.ADT
import io.branchtalk.shared.model.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) sealed trait Permission extends ADT
object Permission extends PermissionCommands {

  case object Administrate extends Permission
  final case class IsUser(userID: ID[User]) extends Permission
  case object ModerateUsers extends Permission
  final case class ModerateChannel(channelID: ID[Channel]) extends Permission
  final case class CanPublish(channelID: ID[Channel]) extends Permission

  implicit val order: Order[Permission] = {
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
