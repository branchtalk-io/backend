package io.branchtalk.users.model

import io.branchtalk.shared.models.{ CreationTime, ID, ModificationTime }
import io.scalaland.chimney.dsl._

final case class UserDao(
  id:                ID[User],
  email:             User.Email,
  username:          User.Name,
  description:       Option[User.Description],
  passwordAlgorithm: Password.Algorithm,
  passwordHash:      Password.Hash,
  passwordSalt:      Password.Salt,
  permissions:       Permissions,
  createdAt:         CreationTime,
  lastModifiedAt:    Option[ModificationTime]
) {

  def toDomain: User =
    User(
      id,
      this
        .into[User.Data]
        .withFieldComputed(_.password, dao => Password(dao.passwordAlgorithm, dao.passwordHash, dao.passwordSalt))
        .transform
    )
}
object UserDao {

  def fromDomain(user: User): UserDao =
    user.data
      .into[UserDao]
      .withFieldConst(_.id, user.id)
      .withFieldComputed(_.passwordAlgorithm, _.password.algorithm)
      .withFieldComputed(_.passwordHash, _.password.hash)
      .withFieldComputed(_.passwordSalt, _.password.salt)
      .transform
}
