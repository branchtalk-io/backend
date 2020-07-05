package io.subfibers.users

import io.subfibers.shared.models.ID

final case class User(
  id: ID[User]
)
