package io.branchtalk.auth

import cats.ApplicativeError
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import io.branchtalk.{ api, users }
import io.branchtalk.mappings._
import io.branchtalk.shared.model.{ CodePosition, CommonError, ID }
import io.branchtalk.users.reads.{ SessionReads, UserReads }

import scala.annotation.unused

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
      session <- sessionReads.requireById(sessionIDApi2Users.get(sessionID))
      user <- userReads.requireById(session.data.userID)
    } yield (user, session)

  private def authCredentials(username: api.Username, password: api.Password) =
    userReads.authenticate(usernameApi2Users.get(username), passwordApi2Users.get(password))

  // TODO: extract all ChannelIDs from required and return
  private def resolveRequired(required: api.RequiredPermissions, owner: Option[api.UserID]) =
    (requiredPermissionsApi2Users(owner.getOrElse(api.UserID.empty)).get(required), Set.empty[ID[users.model.Channel]])
      .pure[F]

  // TODO: fetch bans and filter usedChannelIDs, then add access for each of them to available
  private def resolveAvailable(
    user:                   users.model.User,
    sessionOpt:             Option[users.model.Session],
    @unused usedChannelIDs: Set[ID[users.model.Channel]]
  ) = {
    val allOwnedPermissions = user.data.permissions.append(users.model.Permission.IsUser(user.id))
    sessionOpt
      .map(_.data.usage)
      .collect { case users.model.SessionProperties.Usage.OAuth(permissions) => permissions }
      .fold(allOwnedPermissions)(allOwnedPermissions.intersect)
      .pure[F]
  }

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
      (requiredPermissions, usedChannelIDs) <- resolveRequired(required, owner)
      available <- resolveAvailable(user, sessionOpt, usedChannelIDs)
      _ = logger.trace(
        s"""Validating permissions:
           |required: ${required.show}
           |available: ${available.show}
           |owner: ${owner.show}""".stripMargin
      )
      _ <- ApplicativeError.liftFromOption[F](
        requiredPermissions.some.filter(available.allow),
        CommonError.InsufficientPermissions(
          s"User has insufficient permissions: available ${available.show}, required: ${required.show}",
          CodePosition.providePosition
        )
      )
    } yield (user, sessionOpt)
}
