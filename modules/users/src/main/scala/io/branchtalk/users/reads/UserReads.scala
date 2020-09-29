package io.branchtalk.users.reads

import cats.data.NonEmptySet
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.shared.models
import io.branchtalk.shared.models.{ ID, Paginated }
import io.branchtalk.users.model.{ Password, User }

trait UserReads[F[_]] {

  def authenticate(email: User.Email, password: Password.Raw): F[User]

  def paginate(
    channels: NonEmptySet[models.ID[User]],
    offset:   Long Refined NonNegative,
    limit:    Int Refined Positive
  ): F[Paginated[User]]

  def exists(id: ID[User]): F[Boolean]

  def deleted(id: ID[User]): F[Boolean]

  def getById(id: ID[User]): F[Option[User]]

  def requireById(id: ID[User]): F[User]
}
