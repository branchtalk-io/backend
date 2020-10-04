package io.branchtalk.users.writes

import io.branchtalk.users.model.Session

trait SessionWrites[F[_]] {

  def createSession(newSession:     Session.Create): F[Session]
  def deleteSession(deletedSession: Session.Delete): F[Unit]
}
