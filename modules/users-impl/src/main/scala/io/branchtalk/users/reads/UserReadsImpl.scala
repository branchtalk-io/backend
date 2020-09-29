package io.branchtalk.users.reads

import cats.data.NonEmptySet
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.Paginated
import io.branchtalk.users.model.{ Password, User }

final class UserReadsImpl[F[_]] extends UserReads[F] {

  override def authenticate(email: User.Email, password: Password.Raw): F[User] = ???

  override def paginate(
    channels: NonEmptySet[ID[User]],
    offset:   Long Refined NonNegative,
    limit:    Int Refined Positive
  ): F[Paginated[User]] = ???

  override def exists(id: ID[User]): F[Boolean] = ???

  override def deleted(id: ID[User]): F[Boolean] = ???

  override def getById(id: ID[User]): F[Option[User]] = ???

  override def requireById(id: ID[User]): F[User] = ???
}
