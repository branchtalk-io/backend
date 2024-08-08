package io.branchtalk.users.reads

import io.branchtalk.shared.model.{ ID, Paginated }
import io.branchtalk.users.model.{ Session, User }

trait SessionReads[F[_]] {

  def paginate(
    user:   ID[User],
    sortBy: Session.Sorting,
    offset: Paginated.Offset,
    limit:  Paginated.Limit
  ): F[Paginated[Session]]

  def requireById(id: ID[Session]): F[Session]
}
