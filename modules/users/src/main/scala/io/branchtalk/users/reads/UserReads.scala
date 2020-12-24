package io.branchtalk.users.reads

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.shared.model.{ ID, Paginated }
import io.branchtalk.users.model.{ Password, User }

trait UserReads[F[_]] {

  def authenticate(username: User.Name, password: Password.Raw): F[User]

  def paginate(
    sortBy:  User.Sorting,
    offset:  Long Refined NonNegative,
    limit:   Int Refined Positive,
    filters: List[User.Filter] = List.empty
  ): F[Paginated[User]]

  def exists(id: ID[User]): F[Boolean]

  def deleted(id: ID[User]): F[Boolean]

  def getById(id: ID[User]): F[Option[User]]

  def requireById(id: ID[User]): F[User]
}
