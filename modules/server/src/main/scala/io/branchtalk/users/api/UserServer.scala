package io.branchtalk.users.api

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.mappings._
import io.branchtalk.shared.model.{ CommonError, ID }
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.{ Password, Session, User }
import io.branchtalk.users.reads.{ SessionReads, UserReads }
import io.branchtalk.users.writes.{ SessionWrites, UserWrites }
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

final class UserServer[F[_]: Async](
  authServices:     AuthServices[F],
  userReads:        UserReads[F],
  sessionReads:     SessionReads[F],
  userWrites:       UserWrites[F],
  sessionWrites:    SessionWrites[F],
  paginationConfig: PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  private val sessionExpiresInDays = 7L // make it configurable

  private val serverOptions: Http4sServerOptions[F] = UserServer.serverOptions[F].apply(logger)

  implicit private val errorHandler: ServerErrorHandler[F, UserError] = UserServer.errorHandler[F].apply(logger)

  private val paginate = UserAPIs.paginate.serverLogic[F, User] { case (optOffset, optLimit) =>
    val sortBy = User.Sorting.NameAlphabetically
    val offset = paginationConfig.resolveOffset(optOffset)
    val limit  = paginationConfig.resolveLimit(optLimit)
    for {
      paginated <- userReads.paginate(sortBy, offset.nonNegativeLong, limit.positiveInt)
    } yield Pagination.fromPaginated(paginated.map(APIUser.fromDomain), offset, limit)
  }

  private val newest = UserAPIs.newest.serverLogic[F, User] { case (optOffset, optLimit) =>
    val sortBy = User.Sorting.Newest
    val offset = paginationConfig.resolveOffset(optOffset)
    val limit  = paginationConfig.resolveLimit(optLimit)
    for {
      paginated <- userReads.paginate(sortBy, offset.nonNegativeLong, limit.positiveInt)
    } yield Pagination.fromPaginated(paginated.map(APIUser.fromDomain), offset, limit)
  }

  private val sessions = UserAPIs.sessions.serverLogic[F, User].withUser { case (user, (optOffset, optLimit)) =>
    val sortBy = Session.Sorting.ClosestToExpiry
    val offset = paginationConfig.resolveOffset(optOffset)
    val limit  = paginationConfig.resolveLimit(optLimit)
    for {
      paginated <- sessionReads.paginate(user.id, sortBy, offset.nonNegativeLong, limit.positiveInt)
    } yield Pagination.fromPaginated(paginated.map(APISession.fromDomain), offset, limit)
  }

  private val signUp = UserAPIs.signUp.serverLogic { signup =>
    errorHandler {
      for {
        (user, session) <- userWrites.createUser(
          signup.into[User.Create].withFieldConst(_.password, Password.create(signup.password)).transform
        )
      } yield SignUpResponse(user.id, session.id)
    }
  }

  private val signIn = UserAPIs.signIn.serverLogic[F, (User, Option[Session])].justUser { case (user, sessionOpt) =>
    for {
      session <- sessionOpt match {
        case Some(session) =>
          session.pure[F]
        case None =>
          for {
            expireAt <- Session.ExpirationTime.now[F].map(_.plusDays(sessionExpiresInDays))
            session <- sessionWrites.createSession(
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

  private val signOut = UserAPIs.signOut.serverLogic[F, (User, Option[Session])].justUser { case (user, sessionOpt) =>
    for {
      sessionID <- sessionOpt match {
        case Some(s) => sessionWrites.deleteSession(Session.Delete(s.id)) >> s.id.some.pure[F]
        case None    => none[ID[Session]].pure[F]
      }
    } yield SignOutResponse(userID = user.id, sessionID = sessionID)
  }

  private val fetchProfile = UserAPIs.fetchProfile.serverLogic[F, Option[User]] { userID =>
    for {
      user <- userReads.requireById(userID)
    } yield APIUser.fromDomain(user)
  }

  private val updateProfile = UserAPIs.updateProfile
    .serverLogicWithOwnership[F, User, UserID] { case (userID, _) =>
      userIDApi2Users.reverseGet(userID).pure[F]
    }
    .withUser { case (user, (userID, update)) =>
      val moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
      val data = update
        .into[User.Update]
        .withFieldConst(_.id, userID)
        .withFieldConst(_.moderatorID, moderatorID)
        .withFieldConst(_.newPassword, update.newPassword.map(Password.create))
        .withFieldConst(_.updatePermissions, List.empty)
        .transform
      for {
        _ <- userWrites.updateUser(data)
      } yield UpdateUserResponse(userID)
    }

  private val deleteProfile = UserAPIs.deleteProfile
    .serverLogicWithOwnership[F, User, UserID] { userID =>
      userIDApi2Users.reverseGet(userID).pure[F]
    }
    .withUser { (user, userID) =>
      val moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
      for {
        _ <- userWrites.deleteUser(User.Delete(userID, moderatorID))
      } yield DeleteUserResponse(userID)
    }

  def endpoints: NonEmptyList[ServerEndpoint[Any, F]] = NonEmptyList.of[ServerEndpoint[Any, F]](
    paginate,
    newest,
    sessions,
    signUp,
    signIn,
    signOut,
    fetchProfile,
    updateProfile,
    deleteProfile
  )

  val routes: HttpRoutes[F] = Http4sServerInterpreter(serverOptions).toRoutes(endpoints.toList)
}
object UserServer {

  def serverOptions[F[_]: Sync]: Logger => Http4sServerOptions[F] = ServerOptions.create[F, UserError](
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
        UserError.NotFound(show"$what with id=$id could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        UserError.NotFound(show"Parent $what with id=$id could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        UserError.ValidationFailed(errors)
    }
}
