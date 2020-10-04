package io.branchtalk.users.services

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.models.{ ID, UUIDGenerator }
import io.branchtalk.users.events.{ SessionEvent, UsersEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Session, SessionDao }
import io.scalaland.chimney.dsl._

final class SessionServicesImpl[F[_]: Sync](
  producer:               EventBusProducer[F, UsersEvent],
  transactor:             Transactor[F]
)(implicit UUIDGenerator: UUIDGenerator)
    extends Writes[F, Session, UsersEvent](producer)
    with SessionServices[F] {

  private implicit val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       user_id,
        |       usage_type,
        |       permissions,
        |       expires_at
        |FROM sessions""".stripMargin

  override def createSession(newSession: Session.Create): F[Session] =
    for {
      id <- ID.create[F, Session]
      session = Session(
        id   = id,
        data = newSession.transformInto[Session.Data]
      )
      sessionDao = SessionDao.fromDomain(session)
      _ <- sql"""INSERT INTO sessions (
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
                |)""".stripMargin.update.run.transact(transactor)
      event = session.data.into[SessionEvent.LoggedIn].withFieldConst(_.id, id).transform
      _ <- postEvent(id, UsersEvent.ForSession(event))
    } yield session

  override def requireSession(id: ID[Session]): F[Session] =
    (commonSelect ++ fr"WHERE id = ${id}")
      .query[SessionDao]
      .map(_.toDomain)
      .failNotFound("Session", id)
      .transact(transactor)

  override def deleteSession(deletedSession: Session.Delete): F[Unit] =
    requireSession(deletedSession.id).attempt.flatMap {
      case Left(_) =>
        Sync[F].unit
      case Right(session) =>
        for {
          id <- session.id.pure[F]
          _ <- sql"""DELETE FROM sessions WHERE id = ${deletedSession.id}""".update.run.transact(transactor)
          event = session.data.into[SessionEvent.LoggedOut].withFieldConst(_.id, id).transform
          _ <- postEvent(id, UsersEvent.ForSession(event))
        } yield ()
    }
}
