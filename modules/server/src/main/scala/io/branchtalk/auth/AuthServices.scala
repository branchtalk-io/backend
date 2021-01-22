package io.branchtalk.auth

import cats.ApplicativeError
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import io.branchtalk.{ api, users }
import io.branchtalk.mappings._
import io.branchtalk.shared.model.{ CodePosition, CommonError, ID }
import io.branchtalk.users.reads.{ BanReads, SessionReads, UserReads }

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

final class AuthServicesImpl[F[_]: Sync](userReads: UserReads[F], sessionReads: SessionReads[F], banReads: BanReads[F])
    extends AuthServices[F] {

  private val logger = Logger(getClass)

  private val extractPermissions: api.RequiredPermissions => Set[api.Permission] = {
    case api.RequiredPermissions.Empty        => Set.empty
    case api.RequiredPermissions.AllOf(toSet) => toSet.toSortedSet
    case api.RequiredPermissions.AnyOf(toSet) => toSet.toSortedSet
    case api.RequiredPermissions.And(x, y)    => extractPermissions(x) ++ extractPermissions(y)
    case api.RequiredPermissions.Or(x, y)     => extractPermissions(x) ++ extractPermissions(y)
    case api.RequiredPermissions.Not(x)       => extractPermissions(x)
  }

  private val extractChannelID: api.Permission => Set[ID[users.model.Channel]] = {
    case api.Permission.Administrate               => Set.empty
    case api.Permission.IsOwner                    => Set.empty
    case api.Permission.ModerateUsers              => Set.empty
    case api.Permission.ModerateChannel(channelID) => Set(channelIDApi2Users.get(channelID))
    case api.Permission.CanPublish(channelID)      => Set(channelIDApi2Users.get(channelID))
  }

  private def authSessionID(sessionID: api.SessionID) =
    for {
      session <- sessionReads.requireById(sessionIDApi2Users.get(sessionID))
      user <- userReads.requireById(session.data.userID)
    } yield (user, session)

  private def authCredentials(username: api.Username, password: api.Password) =
    userReads.authenticate(usernameApi2Users.get(username), passwordApi2Users.get(password))

  private def resolveRequired(required: api.RequiredPermissions, owner: Option[api.UserID]) =
    (requiredPermissionsApi2Users(owner.getOrElse(api.UserID.empty)).get(required),
     extractPermissions(required).flatMap(extractChannelID)
    ).pure[F]

  private def resolveAvailable(
    user:           users.model.User,
    sessionOpt:     Option[users.model.Session],
    usedChannelIDs: Set[ID[users.model.Channel]]
  ) =
    banReads.findForUser(user.id).map { bans =>
      val bannedChannels = bans.map(_.scope).flatMap {
        case users.model.Ban.Scope.ForChannel(channelID) => Set(channelID)
        case users.model.Ban.Scope.Globally              => usedChannelIDs
      }
      val allowedChannels     = (usedChannelIDs -- bannedChannels).map(users.model.Permission.CanPublish)
      val allowedOwnProfile   = Set(users.model.Permission.IsUser(user.id))
      val allOwnedPermissions = (allowedChannels ++ allowedOwnProfile).foldLeft(user.data.permissions)(_.append(_))
      sessionOpt
        .map(_.data.usage)
        .collect { case users.model.SessionProperties.Usage.OAuth(permissions) => permissions }
        .fold(allOwnedPermissions)(allOwnedPermissions.intersect)
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
        show"""Validating permissions:
              |required: $required
              |available: $available
              |owner: $owner""".stripMargin
      )
      _ <- ApplicativeError.liftFromOption[F](
        requiredPermissions.some.filter(available.allow),
        CommonError.InsufficientPermissions(
          show"User has insufficient permissions: available $available, required: $required",
          CodePosition.providePosition
        )
      )
    } yield (user, sessionOpt)
}
