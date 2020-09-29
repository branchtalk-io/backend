package io.branchtalk.users.model

import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) final case class User(
  id:   ID[User],
  data: User.Data
)
object User extends UserProperties with UserCommands {

  @Semi(FastEq, ShowPretty) final case class Data(
    email:          User.Email, // validate email
    username:       User.Name,
    description:    Option[User.Description],
    password:       Password,
    permissions:    Permissions,
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  )
}
