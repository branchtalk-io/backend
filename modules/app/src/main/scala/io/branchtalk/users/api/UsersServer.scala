package io.branchtalk.users.api

import cats.effect.{ ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.users.api.UserModels._
import io.branchtalk.shared.models.CommonError
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.branchtalk.users.model.User
import io.branchtalk.users.services.AuthServices
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

final class UsersServer[F[_]: Http4sServerOptions: Sync: ContextShift](
  authServices:     AuthServices[F],
  reads:            UsersReads[F],
  writes:           UsersWrites[F],
  paginationConfig: PaginationConfig
) {

  private val logger = Logger(getClass)

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
        user <- writes.userWrites.createUser(signup.into[User.Create].transform)
      } yield SignUpResponse(user.id, ???) // TODO: figure out session ID creation
    }
  }

  val userRoutes: HttpRoutes[F] = signUp
}
