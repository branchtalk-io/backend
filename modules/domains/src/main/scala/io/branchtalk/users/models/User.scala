package io.branchtalk.users.models

import io.branchtalk.shared.models.{ CreationTime, ID, ModificationTime }

final case class User(
  id:         ID[User],
  username:   Username,
  createdAt:  CreationTime,
  modifiedAt: Option[ModificationTime]
)
