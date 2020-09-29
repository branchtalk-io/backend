package io.branchtalk.users.services

import io.branchtalk.shared.models.ID
import io.branchtalk.users.model.Session

// Sign-in and sign-out should happen immediately so here read-write model separation doesn't make sense
trait SessionServices[F[_]] {

  // TODO: fetch (pagineate?) all user's sessions
  def createSession(newSession:     Session.Create): F[Session]
  def requireSession(id:            ID[Session]):    F[Session]
  def deleteSession(deletedSession: Session.Delete): F[Unit]
}
