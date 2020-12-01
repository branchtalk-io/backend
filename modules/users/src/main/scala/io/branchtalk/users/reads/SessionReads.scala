package io.branchtalk.users.reads

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.shared.model.{ ID, Paginated }
import io.branchtalk.users.model.{ Session, User }

trait SessionReads[F[_]] {

  def paginate(
    user:   ID[User],
    sortBy: Session.Sorting,
    offset: Long Refined NonNegative,
    limit:  Int Refined Positive
  ): F[Paginated[Session]]

  def requireById(id: ID[Session]): F[Session]
}
