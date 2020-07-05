package io.subfibers.users.models

import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }

final case class User(
  id:         ID[User],
  username:   Username,
  createdAt:  CreationTime,
  modifiedAt: Option[ModificationTime]
)
