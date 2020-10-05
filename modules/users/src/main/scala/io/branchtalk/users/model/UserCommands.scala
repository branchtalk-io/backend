package io.branchtalk.users.model

import io.branchtalk.shared.models.{ FastEq, ID, OptionUpdatable, ShowPretty, Updatable }
import io.scalaland.catnip.Semi

trait UserCommands {
  type Create  = UserCommands.Create
  type Update  = UserCommands.Update
  type Delete  = UserCommands.Delete
  type Restore = UserCommands.Restore
  val Create  = UserCommands.Create
  val Update  = UserCommands.Update
  val Delete  = UserCommands.Delete
  val Restore = UserCommands.Restore
}
object UserCommands {

  @Semi(FastEq, ShowPretty) final case class Create(
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    password:    Password
  )

  @Semi(FastEq, ShowPretty) final case class Update(
    id:                ID[User],
    moderatorID:       Option[ID[User]],
    newUsername:       Updatable[User.Name],
    newDescription:    OptionUpdatable[User.Description],
    newPassword:       Updatable[Password],
    updatePermissions: List[Permission.Update]
  )

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:          ID[User],
    moderatorID: Option[ID[User]]
  )

  @Semi(FastEq, ShowPretty) final case class Restore(
    id:          ID[User],
    moderatorID: Option[ID[User]]
  )
}
