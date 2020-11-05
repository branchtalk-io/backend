package io.branchtalk.users.model

import io.branchtalk.ADT
import io.branchtalk.shared.models.{ FastEq, ShowPretty }
import io.scalaland.catnip.Semi

trait PermissionCommands {
  type Update = PermissionCommands.Update
  val Update = PermissionCommands.Update
}
object PermissionCommands {

  @Semi(FastEq, ShowPretty) sealed trait Update extends ADT
  object Update {
    final case class Add(permission: Permission) extends Update
    final case class Remove(permission: Permission) extends Update
  }
}
