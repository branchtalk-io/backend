package io.branchtalk.auth

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import io.branchtalk.api
import io.branchtalk.mappings._
import io.branchtalk.shared.model.{ CodePosition, CommonError }
import io.branchtalk.users
import io.branchtalk.users.reads.{ SessionReads, UserReads }

trait AuthServices[F[_]] {

  def authenticateUser(auth: api.Authentication): F[(users.model.User, Option[users.model.Session])]

  def authorizeUser(
    auth:               api.Authentication,
    requirePermissions: api.RequiredPermissions,
    owner:              Option[api.UserID]
  ): F[(users.model.User, Option[users.model.Session])]
}
object AuthServices {

  @inline def apply[F[_]](implicit authServices: AuthServices[F]): AuthServices[F] = authServices
}

final class AuthServicesImpl[F[_]: Sync](userReads: UserReads[F], sessionReads: SessionReads[F])
    extends AuthServices[F] {

  private val logger = Logger(getClass)

  private def authSessionID(sessionID: api.SessionID) =
    for {
      session <- sessionReads.requireSession(sessionIDApi2Users.get(sessionID))
      user <- userReads.requireById(session.data.userID)
    } yield (user, session)

  private def authCredentials(username: api.Username, password: api.Password) =
    userReads.authenticate(usernameApi2Users.get(username), passwordApi2Users.get(password))

  override def authenticateUser(
    auth: api.Authentication
  ): F[(users.model.User, Option[users.model.Session])] = auth.fold(
    sessionID => authSessionID(sessionID).map { case (user, session) => user -> session.some },
    (username, password) => authCredentials(username, password).map(_ -> none[users.model.Session])
  )

  override def authorizeUser(
    auth:     api.Authentication,
    required: api.RequiredPermissions,
    owner:    Option[api.UserID]
  ): F[(users.model.User, Option[users.model.Session])] =
    for {
      (user, sessionOpt) <- authenticateUser(auth)
      allOwnedPermissions = user.data.permissions.append(users.model.Permission.IsUser(user.id))
      available = sessionOpt
        .map(_.data.usage)
        .collect { case users.model.SessionProperties.Usage.OAuth(permissions) => permissions }
        .fold(allOwnedPermissions)(allOwnedPermissions.intersect)
      _ = logger.trace(
        s"""Validating permissions:
           |required: ${required.show}
           |available: ${available.show}
           |owner: ${owner.show}""".stripMargin
      )
      _ <-
        if (available.allow(requiredPermissionsApi2Users(owner.getOrElse(api.UserID.empty)).get(required))) Sync[F].unit
        else {
          (CommonError.InsufficientPermissions(
            s"User has insufficient permissions: available ${available.show}, required: ${required.show}",
            CodePosition.providePosition
          ): Throwable).raiseError[F, Unit]
        }
    } yield (user, sessionOpt)
}
