package io.branchtalk.users.api

import cats.data.NonEmptyList
import cats.effect.{ Clock, Concurrent, ContextShift, Sync, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.mappings._
import io.branchtalk.shared.model.{ CommonError, ID }
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.branchtalk.users.model.{ Password, Session, User }
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

final class UserServer[F[_]: Sync: ContextShift: Clock: Concurrent: Timer](
  authServices:     AuthServices[F],
  usersReads:       UsersReads[F],
  usersWrites:      UsersWrites[F],
  paginationConfig: PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  private val sessionExpiresInDays = 7L // make it configurable

  implicit private val serverOptions: Http4sServerOptions[F] = UserServer.serverOptions[F].apply(logger)

  implicit private val errorHandler: ServerErrorHandler[F, UserError] = UserServer.errorHandler[F].apply(logger)

  private val paginate = UserAPIs.paginate.serverLogic[F].apply { case ((_, _), optOffset, optLimit) =>
    val sortBy = User.Sorting.NameAlphabetically
    val offset = paginationConfig.resolveOffset(optOffset)
    val limit  = paginationConfig.resolveLimit(optLimit)
    for {
      paginated <- usersReads.userReads.paginate(sortBy, offset.nonNegativeLong, limit.positiveInt)
    } yield Pagination.fromPaginated(paginated.map(APIUser.fromDomain), offset, limit)
  }

  private val newest = UserAPIs.newest.serverLogic[F].apply { case ((_, _), optOffset, optLimit) =>
    val sortBy = User.Sorting.Newest
    val offset = paginationConfig.resolveOffset(optOffset)
    val limit  = paginationConfig.resolveLimit(optLimit)
    for {
      paginated <- usersReads.userReads.paginate(sortBy, offset.nonNegativeLong, limit.positiveInt)
    } yield Pagination.fromPaginated(paginated.map(APIUser.fromDomain), offset, limit)
  }

  private val signUp = UserAPIs.signUp.serverLogic { signup =>
    errorHandler {
      for {
        (user, session) <- usersWrites.userWrites.createUser(
          signup.into[User.Create].withFieldConst(_.password, Password.create(signup.password)).transform
        )
      } yield SignUpResponse(user.id, session.id)
    }
  }

  private val signIn = UserAPIs.signIn.serverLogic[F].apply { case (user, sessionOpt) =>
    for {
      session <- sessionOpt match {
        case Some(session) =>
          session.pure[F]
        case None =>
          for {
            expireAt <- Session.ExpirationTime.now[F].map(_.plusDays(sessionExpiresInDays))
            session <- usersWrites.sessionWrites.createSession(
              Session.Create(
                userID = user.id,
                usage = Session.Usage.UserSession,
                expiresAt = expireAt
              )
            )
          } yield session
      }
    } yield session.data.into[SignInResponse].withFieldConst(_.sessionID, session.id).transform
  }

  private val signOut = UserAPIs.signOut.serverLogic[F].apply { case (user, sessionOpt) =>
    for {
      sessionID <- sessionOpt match {
        case Some(s) => usersWrites.sessionWrites.deleteSession(Session.Delete(s.id)) >> s.id.some.pure[F]
        case None    => none[ID[Session]].pure[F]
      }
    } yield SignOutResponse(userID = user.id, sessionID = sessionID)
  }

  private val fetchProfile = UserAPIs.fetchProfile.serverLogic[F].apply { case ((_, _), userID) =>
    for {
      user <- usersReads.userReads.requireById(userID)
    } yield APIUser.fromDomain(user)
  }

  private val updateProfile = UserAPIs.updateProfile
    .serverLogicWithOwnership[F, UserID]
    .apply { case (_, userID, _) =>
      userIDApi2Users.reverseGet(userID).pure[F]
    } { case ((user, _), userID, update) =>
      val moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
      val data = update
        .into[User.Update]
        .withFieldConst(_.id, userID)
        .withFieldConst(_.moderatorID, moderatorID)
        .withFieldConst(_.newPassword, update.newPassword.map(Password.create))
        .withFieldConst(_.updatePermissions, List.empty)
        .transform
      for {
        _ <- usersWrites.userWrites.updateUser(data)
      } yield UpdateUserResponse(userID)
    }

  private val deleteProfile = UserAPIs.deleteProfile
    .serverLogicWithOwnership[F, UserID]
    .apply { case (_, userID) =>
      userIDApi2Users.reverseGet(userID).pure[F]
    } { case ((user, _), userID) =>
      val moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
      for {
        _ <- usersWrites.userWrites.deleteUser(User.Delete(userID, moderatorID))
      } yield DeleteUserResponse(userID)
    }

  def endpoints: NonEmptyList[ServerEndpoint[_, UserError, _, Any, F]] = NonEmptyList.of(
    paginate,
    newest,
    signUp,
    signIn,
    signOut,
    fetchProfile,
    updateProfile,
    deleteProfile
  )

  val routes: HttpRoutes[F] = endpoints.map(_.toRoutes).reduceK
}
object UserServer {

  def serverOptions[F[_]: Sync: ContextShift]: Logger => Http4sServerOptions[F] = ServerOptions.create[F, UserError](
    _,
    ServerOptions.ErrorHandler[UserError](
      () => UserError.ValidationFailed(NonEmptyList.one("Data missing")),
      () => UserError.ValidationFailed(NonEmptyList.one("Multiple errors")),
      (msg, _) => UserError.ValidationFailed(NonEmptyList.one(s"Error happened: ${msg}")),
      (expected, actual) => UserError.ValidationFailed(NonEmptyList.one(s"Expected: $expected, actual: $actual")),
      errors =>
        UserError.ValidationFailed(
          NonEmptyList
            .fromList(errors.map(e => s"Invalid value at ${e.path.map(_.encodedName).mkString(".")}"))
            .getOrElse(NonEmptyList.one("Validation failed"))
        )
    )
  )

  def errorHandler[F[_]: Sync]: Logger => ServerErrorHandler[F, UserError] =
    ServerErrorHandler.handleCommonErrors[F, UserError] {
      case CommonError.InvalidCredentials(_) =>
        UserError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        UserError.NoPermission(msg)
      case CommonError.NotFound(what, id, _) =>
        UserError.NotFound(s"$what with id=${id.show} could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        UserError.NotFound(s"Parent $what with id=${id.show} could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        UserError.ValidationFailed(errors)
    }
}
