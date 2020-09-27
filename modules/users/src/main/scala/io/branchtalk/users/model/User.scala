package io.branchtalk.users.model

import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty }
import io.scalaland.catnip.Semi

// TODO: session entity

@Semi(FastEq, ShowPretty) final case class User(
  id:   ID[User],
  data: User.Data
)
object User extends UserProperties with UserCommands {

  @Semi(FastEq, ShowPretty) final case class Data(
    email:          User.Email,
    description:    User.Description,
    password:       Password,
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  )
}
