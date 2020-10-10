package io.branchtalk.users.services

import cats.effect.Sync
import io.branchtalk.api.Authentication
import io.branchtalk.users.model.User
import io.branchtalk.users.api.UserAPIs
import io.branchtalk.users.reads.{ SessionReads, UserReads }

trait AuthServices[F[_]] {

  def authUser(auth: Authentication): F[User]
}

final class AuthServicesImpl[F[_]: Sync](userReads: UserReads[F], sessionReads: SessionReads[F])
    extends AuthServices[F] {

  override def authUser(auth: Authentication): F[User] = auth match {
    case Authentication.Session(sessionID) =>
      sessionReads.requireSession(UserAPIs.sessionIDMapping.get(sessionID)).flatMap { session =>
        userReads.requireById(session.data.userID)
      }
    case Authentication.Credentials(username, password) =>
      userReads.authenticate(UserAPIs.usernameMapping.get(username), UserAPIs.passwordMapping.get(password))
  }
}
