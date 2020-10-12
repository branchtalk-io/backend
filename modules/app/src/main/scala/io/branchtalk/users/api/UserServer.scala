package io.branchtalk.users.api

import cats.effect.{ Clock, ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CommonError, ID }
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.branchtalk.users.model.{ Session, User }
import io.branchtalk.users.services.AuthServices
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

final class UserServer[F[_]: Http4sServerOptions: Sync: ContextShift: Clock](
  authServices:     AuthServices[F],
  usersReads:       UsersReads[F],
  usersWrites:      UsersWrites[F],
  paginationConfig: PaginationConfig
) {

  private val logger = Logger(getClass)

  private val sessionExpiresInDays = 7L // make it configurable

  private def withErrorHandling[A](fa: F[A]): F[Either[UserError, A]] = fa.map(_.asRight[UserError]).handleErrorWith {
    case CommonError.InvalidCredentials(_) =>
      (UserError.BadCredentials("Invalid credentials"): UserError).asLeft[A].pure[F]
    case CommonError.InsufficientPermissions(msg, _) =>
      (UserError.NoPermission(msg): UserError).asLeft[A].pure[F]
    case CommonError.NotFound(what, id, _) =>
      (UserError.NotFound(s"$what with id=${id.show} could not be found"): UserError).asLeft[A].pure[F]
    case CommonError.ParentNotExist(what, id, _) =>
      (UserError.NotFound(s"Parent $what with id=${id.show} could not be found"): UserError).asLeft[A].pure[F]
    case CommonError.ValidationFailed(errors, _) =>
      (UserError.ValidationFailed(errors): UserError).asLeft[A].pure[F]
    case error: Throwable =>
      logger.warn("Unhandled error in domain code", error)
      error.raiseError[F, Either[UserError, A]]
  }

  private val signUp = UserAPIs.signUp.toRoutes { signup =>
    withErrorHandling {
      for {
        (user, session) <- usersWrites.userWrites.createUser(signup.into[User.Create].transform)
      } yield SignUpResponse(user.id, session.id)
    }
  }

  private val signIn = UserAPIs.signIn.toRoutes { authentication =>
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

  private val signOut = UserAPIs.signOut.toRoutes { authentication =>
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

  // TODO: optional auth for logging or sth
  private val fetchProfile = UserAPIs.fetchProfile.toRoutes { userID =>
    withErrorHandling {
      for {
        user <- usersReads.userReads.requireById(userID)
      } yield APIUser.fromDomain(user)
    }
  }

  private val updateProfile = UserAPIs.updateProfile.toRoutes {
    case (authentication, userID, update) =>
      withErrorHandling {
        for {
          user <- authServices.authorizeUser(authentication, api.Permission.EditProfile(userIDApi2Users(userID)))
          moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
          data = update
            .into[User.Update]
            .withFieldConst(_.id, userID)
            .withFieldConst(_.moderatorID, moderatorID)
            .withFieldConst(_.updatePermissions, List.empty)
            .transform
          _ <- usersWrites.userWrites.updateUser(data)
        } yield UpdateUserResponse(userID)
      }
  }

  private val deleteProfile = UserAPIs.deleteProfile.toRoutes {
    case (authentication, userID) =>
      withErrorHandling {
        for {
          user <- authServices.authorizeUser(authentication, api.Permission.EditProfile(userIDApi2Users(userID)))
          moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
          _ <- usersWrites.userWrites.deleteUser(User.Delete(userID, moderatorID))
        } yield DeleteUserResponse(userID)
      }
  }

  val userRoutes: HttpRoutes[F] = signUp <+> signIn <+> signOut <+> fetchProfile <+> updateProfile <+> deleteProfile
}
