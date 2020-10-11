package io.branchtalk.users.api

import cats.effect.{ Clock, ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.users.api.UserModels._
import io.branchtalk.shared.models.{ CommonError, ID }
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.branchtalk.users.model.{ Session, User }
import io.branchtalk.users.services.AuthServices
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

final class UserServer[F[_]: Http4sServerOptions: Sync: ContextShift: Clock](
  authServices:     AuthServices[F],
  reads:            UsersReads[F],
  writes:           UsersWrites[F],
  paginationConfig: PaginationConfig
) {

  private val logger = Logger(getClass)

  private val sessionExpiresInDays = 7L // make it configurable

  private def withErrorHandling[A](fa: F[A]): F[Either[UserError, A]] = fa.map(_.asRight[UserError]).handleErrorWith {
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
        (user, session) <- writes.userWrites.createUser(signup.into[User.Create].transform)
      } yield SignUpResponse(user.id, session.id)
    }
  }

  private val signIn = UserAPIs.signIn.toRoutes { authentication =>
    withErrorHandling {
      for {
        (user, sessionOpt) <- authServices.authUserSession(authentication)
        session <- sessionOpt match {
          case Some(session) =>
            session.pure[F]
          case None =>
            for {
              expireAt <- Session.ExpirationTime.now[F].map(_.plusDays(sessionExpiresInDays))
              session <- writes.sessionWrites.createSession(
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
        (user, sessionOpt) <- authServices.authUserSession(authentication)
        sessionID <- sessionOpt match {
          case Some(s) => writes.sessionWrites.deleteSession(Session.Delete(s.id)) >> s.id.some.pure[F]
          case None    => none[ID[Session]].pure[F]
        }
      } yield SignOutResponse(userID = user.id, sessionID = sessionID)
    }
  }

  private val fetchProfile = UserAPIs.fetchProfile.toRoutes { userID =>
    withErrorHandling {
      for {
        user <- reads.userReads.requireById(userID)
      } yield APIUser.fromDomain(user)
    }
  }

  // TODO: updateProfile

  private val deleteProfile = UserAPIs.deleteProfile.toRoutes {
    case (authentication, userID) =>
      withErrorHandling {
        for {
          user <- authServices.authUser(authentication)
          moderatorID = if (user.id === userID) none[ID[User]] else user.id.some
          _ <- writes.userWrites.deleteUser(User.Delete(userID, moderatorID))
        } yield DeleteUserResponse(userID)
      }
  }

  val userRoutes: HttpRoutes[F] = signUp <+> signIn <+> signOut <+> fetchProfile <+> deleteProfile
}
