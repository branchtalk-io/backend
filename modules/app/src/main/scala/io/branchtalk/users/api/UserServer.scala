package io.branchtalk.users.api

import cats.data.NonEmptyList
import cats.effect.{ Clock, ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api
import io.branchtalk.api.ServerErrorHandling
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CommonError, ID }
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.branchtalk.users.model.{ Password, Session, User }
import io.branchtalk.users.services.AuthServices
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

final class UserServer[F[_]: Http4sServerOptions: Sync: ContextShift: Clock](
  authServices:     AuthServices[F],
  usersReads:       UsersReads[F],
  usersWrites:      UsersWrites[F],
  paginationConfig: PaginationConfig
) {

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

  private val signUp = UserAPIs.signUp.serverLogic { signup =>
    withErrorHandling {
      for {
        (user, session) <- usersWrites.userWrites.createUser(
          signup.into[User.Create].withFieldConst(_.password, Password.create(signup.password)).transform
        )
      } yield SignUpResponse(user.id, session.id)
    }
  }

  private val signIn = UserAPIs.signIn.serverLogic { authentication =>
    withErrorHandling {
      for {
        (user, sessionOpt) <- authServices.authenticateUserWithSessionOpt(authentication)
        session <- sessionOpt match {
          case Some(session) =>
            session.pure[F]
          case None =>
            for {
              expireAt <- Session.ExpirationTime.now[F].map(_.plusDays(sessionExpiresInDays))
              session <- usersWrites.sessionWrites.createSession(
                Session.Create(
                  userID    = user.id,
                  usage     = Session.Usage.UserSession,
                  expiresAt = expireAt
                )
              )
            } yield session
        }
      } yield session.data.into[SignInResponse].withFieldConst(_.sessionID, session.id).transform
    }
  }

  private val signOut = UserAPIs.signOut.serverLogic { authentication =>
    withErrorHandling {
      for {
        (user, sessionOpt) <- authServices.authenticateUserWithSessionOpt(authentication)
        sessionID <- sessionOpt match {
          case Some(s) => usersWrites.sessionWrites.deleteSession(Session.Delete(s.id)) >> s.id.some.pure[F]
          case None    => none[ID[Session]].pure[F]
        }
      } yield SignOutResponse(userID = user.id, sessionID = sessionID)
    }
  }

  private val fetchProfile = UserAPIs.fetchProfile.serverLogic { userID =>
    withErrorHandling {
      for {
        user <- usersReads.userReads.requireById(userID)
      } yield APIUser.fromDomain(user)
    }
  }

  private val updateProfile = UserAPIs.updateProfile.serverLogic {
    case (authentication, userID, update) =>
      withErrorHandling {
        for {
          user <- authServices.authorizeUser(authentication, api.Permission.EditProfile(userIDApi2Users(userID)))
          moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
          data = update
            .into[User.Update]
            .withFieldConst(_.id, userID)
            .withFieldConst(_.moderatorID, moderatorID)
            .withFieldConst(_.newPassword, update.newPassword.map(Password.create))
            .withFieldConst(_.updatePermissions, List.empty)
            .transform
          _ <- usersWrites.userWrites.updateUser(data)
        } yield UpdateUserResponse(userID)
      }
  }

  private val deleteProfile = UserAPIs.deleteProfile.serverLogic {
    case (authentication, userID) =>
      withErrorHandling {
        for {
          user <- authServices.authorizeUser(authentication, api.Permission.EditProfile(userIDApi2Users(userID)))
          moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
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

  val routes: HttpRoutes[F] = endpoints.map(_.toRoutes).reduceK
}
