package io.branchtalk.users.model

import io.branchtalk.shared.model.{ ID, SensitiveData }

final case class SensitiveDataKeyDao(
  userID:    ID[User],
  key:       SensitiveData.Key,
  algorithm: SensitiveData.Algorithm
)
