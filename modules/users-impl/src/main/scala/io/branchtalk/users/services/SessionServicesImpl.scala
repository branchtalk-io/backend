package io.branchtalk.users.services

import io.branchtalk.users.model.Session

final class SessionServicesImpl[F[_]] extends SessionServices[F] {

  override def createSession(newSession: Session.Create): F[Session] = ???

  override def deleteSession(deletedSession: Session.Delete): F[Unit] = ???
}
