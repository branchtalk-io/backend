package io.branchtalk.users.services

import cats.effect.Sync
import io.branchtalk.api.{ Authentication, Password, SessionID, Username }
import io.branchtalk.users.model.{ Session, User }
import io.branchtalk.users.api.UserAPIs
import io.branchtalk.users.reads.{ SessionReads, UserReads }

trait AuthServices[F[_]] {

  def authUser(auth:        Authentication): F[User]
  def authUserSession(auth: Authentication): F[(User, Option[Session])]
}

final class AuthServicesImpl[F[_]: Sync](userReads: UserReads[F], sessionReads: SessionReads[F])
    extends AuthServices[F] {

  private def authSessionID(sessionID: SessionID) =
    for {
      session <- sessionReads.requireSession(UserAPIs.sessionIDMapping.get(sessionID))
      user <- userReads.requireById(session.data.userID)
    } yield (user, session)

  private def authCredentials(username: Username, password: Password) =
    userReads.authenticate(UserAPIs.usernameMapping.get(username), UserAPIs.passwordMapping.get(password))

  override def authUser(auth: Authentication): F[User] = auth.fold(authSessionID(_).map(_._1), authCredentials)

  override def authUserSession(auth: Authentication): F[(User, Option[Session])] = auth.fold(
    sessionID => authSessionID(sessionID).map { case (user, session) => user -> session.some },
    (username, password) => authCredentials(username, password).map(user => user -> none[Session])
  )
}
