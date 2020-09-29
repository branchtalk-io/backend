package io.branchtalk.users.services

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models.{ ID, UUIDGenerator }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Session, SessionDao }
import io.scalaland.chimney.dsl._

// TODO: makes sense to push login/logout events to event bus when we'll define them
final class SessionServicesImpl[F[_]: Sync](transactor: Transactor[F])(implicit UUIDGenerator: UUIDGenerator)
    extends SessionServices[F] {

  private implicit val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       user_id,
        |       usage_type,
        |       permissions,
        |       expires_at
        |FROM sessions""".stripMargin

  override def createSession(newSession: Session.Create): F[Session] =
    ID.create[F, Session]
      .map { id =>
        Session(
          id   = id,
          data = newSession.transformInto[Session.Data]
        )
      }
      .flatMap { session =>
        val sessionDao = SessionDao.fromDomain(session)
        sql"""INSERT INTO sessions (
             |  id,
             |  user_id,
             |  usage_type,
             |  permissions,
             |  expires_at
             |)
             |VALUES (
             |  ${sessionDao.id},
             |  ${sessionDao.userID},
             |  ${sessionDao.usageType},
             |  ${sessionDao.usagePermissions},
             |  ${sessionDao.expiresAt}
             |)""".stripMargin.update.run.transact(transactor) >> session.pure[F]
      }

  override def requireSession(id: ID[Session]): F[Session] =
    (commonSelect ++ fr"WHERE id = ${id}")
      .query[SessionDao]
      .map(_.toDomain)
      .failNotFound("Session", id)
      .transact(transactor)

  override def deleteSession(deletedSession: Session.Delete): F[Unit] =
    sql"""DELETE FROM sessions WHERE id = ${deletedSession.id}""".update.run.transact(transactor).void
}
