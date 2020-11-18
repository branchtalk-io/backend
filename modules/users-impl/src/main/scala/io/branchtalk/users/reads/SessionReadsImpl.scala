package io.branchtalk.users.reads

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model.ID
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Session, SessionDao }

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

  override def requireSession(id: ID[Session]): F[Session] =
    (commonSelect ++ fr"WHERE id = ${id}")
      .query[SessionDao]
      .map(_.toDomain)
      .failNotFound("Session", id)
      .transact(transactor)
}
