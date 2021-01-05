package io.branchtalk.users.writes

import cats.effect.Sync
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.model.{ ID, UUIDGenerator }
import io.branchtalk.users.events.{ SessionEvent, UsersEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Session, SessionDao }
import io.branchtalk.users.reads.SessionReadsImpl
import io.scalaland.chimney.dsl._

final class SessionWritesImpl[F[_]: Sync: MDC](
  producer:   EventBusProducer[F, UsersEvent],
  transactor: Transactor[F]
)(implicit
  uuidGenerator: UUIDGenerator
) extends Writes[F, Session, UsersEvent](producer)
    with SessionWrites[F] {

  private val reads = new SessionReadsImpl[F](transactor)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def createSession(newSession: Session.Create): F[Session] =
    for {
      id <- ID.create[F, Session]
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      session = Session(
        id = id,
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
      event = session.data
        .into[SessionEvent.LoggedIn]
        .withFieldConst(_.id, id)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, UsersEvent.ForSession(event))
    } yield session

  override def deleteSession(deletedSession: Session.Delete): F[Unit] =
    reads.requireById(deletedSession.id).attempt.flatMap {
      case Left(_) =>
        Sync[F].unit
      case Right(session) =>
        for {
          correlationID <- CorrelationID.getCurrentOrGenerate[F]
          id = session.id
          _ <- sql"""DELETE FROM sessions WHERE id = ${deletedSession.id}""".update.run.transact(transactor)
          event = session.data
            .into[SessionEvent.LoggedOut]
            .withFieldConst(_.id, id)
            .withFieldConst(_.correlationID, correlationID)
            .transform
          _ <- postEvent(id, UsersEvent.ForSession(event))
        } yield ()
    }
}
