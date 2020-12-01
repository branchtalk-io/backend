package io.branchtalk.users.reads

import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model.{ ID, Paginated }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Session, SessionDao, User }

final class SessionReadsImpl[F[_]: Sync](
  transactor: Transactor[F]
) extends SessionReads[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

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
    offset: Long Refined NonNegative,
    limit:  Int Refined Positive
  ): F[Paginated[Session]] =
    (commonSelect ++ fr"WHERE user_id = ${user}" ++ orderBy(sortBy))
      .paginate[SessionDao](offset, limit)
      .map(_.map(_.toDomain))
      .transact(transactor)

  override def requireSession(id: ID[Session]): F[Session] =
    (commonSelect ++ fr"WHERE id = ${id}")
      .query[SessionDao]
      .map(_.toDomain)
      .failNotFound("Session", id)
      .transact(transactor)
}
