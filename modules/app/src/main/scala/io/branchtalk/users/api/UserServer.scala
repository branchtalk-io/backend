package io.branchtalk.users.api

import cats.data.NonEmptyList
import cats.effect.{ Clock, Concurrent, ContextShift, Sync, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CommonError, ID }
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.branchtalk.users.model.{ Password, Session, User }
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

final class UserServer[F[_]: Http4sServerOptions: Sync: ContextShift: Clock: Concurrent: Timer](
  authServices:     AuthServices[F],
  usersReads:       UsersReads[F],
  usersWrites:      UsersWrites[F],
  paginationConfig: PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  private val sessionExpiresInDays = 7L // make it configurable

  private val withErrorHandling = ServerErrorHandling.handleCommonErrors[F, UserError] {
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
  }(logger)

  private val signUp: ServerEndpoint[SignUpRequest, UserError, SignUpResponse, Nothing, F] =
    UserAPIs.signUp.serverLogic { signup =>
      withErrorHandling {
        for {
          (user, session) <- usersWrites.userWrites.createUser(
            signup.into[User.Create].withFieldConst(_.password, Password.create(signup.password)).transform
          )
        } yield SignUpResponse(user.id, session.id)
      }
    }

  private val signIn: ServerEndpoint[Authentication, UserError, SignInResponse, Nothing, F] =
    UserAPIs.signIn.authenticated.serverLogic[F] { case (user, sessionOpt) =>
      withErrorHandling {
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
    }

  private val signOut: ServerEndpoint[Authentication, UserError, SignOutResponse, Nothing, F] =
    UserAPIs.signOut.authenticated.serverLogic { case (user, sessionOpt) =>
      withErrorHandling {
        for {
          sessionID <- sessionOpt match {
            case Some(s) => usersWrites.sessionWrites.deleteSession(Session.Delete(s.id)) >> s.id.some.pure[F]
            case None    => none[ID[Session]].pure[F]
          }
        } yield SignOutResponse(userID = user.id, sessionID = sessionID)
      }
    }

  private val fetchProfile: ServerEndpoint[ID[User], UserError, APIUser, Nothing, F] =
    UserAPIs.fetchProfile.serverLogic { userID =>
      withErrorHandling {
        for {
          user <- usersReads.userReads.requireById(userID)
        } yield APIUser.fromDomain(user)
      }
    }

  private val updateProfile: ServerEndpoint[
    (Authentication, ID[User], UpdateUserRequest, RequiredPermissions),
    UserError,
    UpdateUserResponse,
    Nothing,
    F
  ] = UserAPIs.updateProfile.authorized
    .withOwnership { case (_, userID, _, _) =>
      userIDApi2Users.reverseGet(userID).pure[F]
    }
    .serverLogic { case ((user, _), userID, update) =>
      withErrorHandling {
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
    }

  private val deleteProfile: ServerEndpoint[
    (Authentication, ID[User], RequiredPermissions),
    UserError,
    DeleteUserResponse,
    Nothing,
    F
  ] = UserAPIs.deleteProfile.authorized
    .withOwnership { case (_, userID, _) =>
      userIDApi2Users.reverseGet(userID).pure[F]
    }
    .serverLogic { case ((user, _), userID) =>
      withErrorHandling {
        val moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
        for {
          _ <- usersWrites.userWrites.deleteUser(User.Delete(userID, moderatorID))
        } yield DeleteUserResponse(userID)
      }
    }

  def endpoints: NonEmptyList[ServerEndpoint[_, UserError, _, Nothing, F]] = NonEmptyList.of(
    signUp,
    signIn,
    signOut,
    fetchProfile,
    updateProfile,
    deleteProfile
  )

  val routes: HttpRoutes[F] = endpoints.map(_.asR[Fs2Streams[F] with WebSockets].toRoutes).reduceK
}
