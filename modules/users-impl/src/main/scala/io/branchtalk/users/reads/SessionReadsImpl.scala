package io.branchtalk.users.reads

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.{ ID, Paginated }
import io.branchtalk.users.infrastructure.DoobieExtensions.{ *, given }
import io.branchtalk.users.model.{ Session, SessionDao, User }

final class SessionReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends SessionReads[F] {

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       user_id,
        |       usage_type,
        |       permissions,
        |       expires_at
        |FROM sessions""".stripMargin

  private val orderBy: Session.Sorting => Fragment = { case Session.Sorting.ClosestToExpiry =>
    fr"ORDER BY expires_at DESC"
  }

  override def paginate(
    user:   ID[User],
    sortBy: Session.Sorting,
    offset: Paginated.Offset,
    limit:  Paginated.Limit
  ): F[Paginated[Session]] =
    (commonSelect ++ fr"WHERE user_id = $user" ++ orderBy(sortBy))
      .paginate[SessionDao](offset, limit, show"Paginate Users' Session from $offset taking $limit sorted by $sortBy")
      .map(_.map(_.toDomain))
      .transact(transactor)

  override def requireById(id: ID[Session]): F[Session] =
    (commonSelect ++ fr"WHERE id = $id")
      .queryWithLabel[SessionDao](show"Require Users' Session by ID=$id")
      .map(_.toDomain)
      .failNotFound("Session", id)
      .transact(transactor)
}
