package io.branchtalk.auth

import cats.effect.Sync
import io.branchtalk.shared.models.{ CodePosition, CommonError }
import io.branchtalk.{ api, users }
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint

trait AuthenticateLogic[I, I2, E, O, S] { self =>
  protected type Rest
  protected val mapped:      Endpoint[I, E, O, S]
  protected val extractAuth: I => (Rest, api.Authentication)
  protected val addUser:     (Rest, (users.model.User, Option[users.model.Session])) => I2

  def serverLogic[F[_]: Sync: AuthServices](f: I2 => F[Either[E, O]]): ServerEndpoint[I, E, O, S, F] =
    mapped.serverLogic[F] { i =>
      val (rest, auth) = extractAuth(i)
      auth.pipe(AuthServices[F].authenticateUser).map(addUser(rest, _)).flatMap(f)
    }

  def withOwnership[F[_]: Sync: AuthServices](
    f:                     I => F[Unit]
  )(implicit codePosition: CodePosition): AuthenticateOwnedLogic[F, I, I2, E, O, S] =
    new AuthenticateOwnedLogic[F, I, I2, E, O, S] {
      protected type Rest = self.Rest
      protected val mapped      = self.mapped
      protected val extractAuth = self.extractAuth
      protected val addUser     = self.addUser
      protected val ownership   = f
    }
}

abstract class AuthenticateOwnedLogic[F[_]: Sync: AuthServices, I, I2, E, O, S](implicit codePosition: CodePosition) {
  protected type Rest
  protected val mapped:      Endpoint[I, E, O, S]
  protected val extractAuth: I => (Rest, api.Authentication)
  protected val addUser:     (Rest, (users.model.User, Option[users.model.Session])) => I2
  protected val ownership:   I => F[Unit]

  def serverLogic(f: I2 => F[Either[E, O]]): ServerEndpoint[I, E, O, S, F] =
    mapped.serverLogic[F] { i =>
      val (rest, auth) = extractAuth(i)
      ownership(i).handleErrorWith(_ =>
        Sync[F].raiseError(CommonError.InsufficientPermissions("Ownership was not confirmed", codePosition))
      ) >> auth.pipe(AuthServices[F].authenticateUser).map(addUser(rest, _)).flatMap(f)
    }
}

trait OptAuthenticateLogic[I, I2, E, O, S] { self =>
  protected type Rest
  protected val mapped:      Endpoint[I, E, O, S]
  protected val extractAuth: I => (Rest, Option[api.Authentication])
  protected val addUser:     (Rest, (Option[users.model.User], Option[users.model.Session])) => I2

  def serverLogic[F[_]: Sync: AuthServices](f: I2 => F[Either[E, O]]): ServerEndpoint[I, E, O, S, F] =
    mapped.serverLogic[F] { i =>
      val (rest, auth) = extractAuth(i)
      auth
        .traverse(AuthServices[F].authenticateUser)
        .map { optUserSession =>
          optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
        }
        .map(addUser(rest, _))
        .flatMap(f)
    }

  def withOwnership[F[_]: Sync: AuthServices](
    f:                     I => F[Unit]
  )(implicit codePosition: CodePosition): OptAuthenticateOwnedLogic[F, I, I2, E, O, S] =
    new OptAuthenticateOwnedLogic[F, I, I2, E, O, S] {
      protected type Rest = self.Rest
      protected val mapped      = self.mapped
      protected val extractAuth = self.extractAuth
      protected val addUser     = self.addUser
      protected val ownership   = f
    }
}

abstract class OptAuthenticateOwnedLogic[F[_]: Sync: AuthServices, I, I2, E, O, S](implicit
  codePosition: CodePosition
) {
  protected type Rest
  protected val mapped:      Endpoint[I, E, O, S]
  protected val extractAuth: I => (Rest, Option[api.Authentication])
  protected val addUser:     (Rest, (Option[users.model.User], Option[users.model.Session])) => I2
  protected val ownership:   I => F[Unit]

  def serverLogic(f: I2 => F[Either[E, O]]): ServerEndpoint[I, E, O, S, F] =
    mapped.serverLogic[F] { i =>
      val (rest, auth) = extractAuth(i)
      ownership(i).handleErrorWith(_ =>
        Sync[F].raiseError(CommonError.InsufficientPermissions("Ownership was not confirmed", codePosition))
      ) >> auth
        .traverse(AuthServices[F].authenticateUser)
        .map { optUserSession =>
          optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
        }
        .map(addUser(rest, _))
        .flatMap(f)
    }
}

trait AuthorizeLogic[I, I2, E, O, S] { self =>
  protected type Rest1
  protected type Rest2
  protected val mapped:      Endpoint[I, E, O, S]
  protected val extractAuth: I => (Rest1, api.Authentication)
  protected val extractPerm: Rest1 => (Rest2, api.RequiredPermissions)
  protected val addUser:     (Rest2, (users.model.User, Option[users.model.Session])) => I2

  def serverLogic[F[_]: Sync: AuthServices](f: I2 => F[Either[E, O]]): ServerEndpoint[I, E, O, S, F] =
    mapped.serverLogic[F] { i =>
      val (rest1, auth) = extractAuth(i)
      val (rest2, perm) = extractPerm(rest1)
      AuthServices[F].authorizeUser(auth, perm, None).map(addUser(rest2, _)).flatMap(f)
    }

  def withOwnership[F[_]: Sync: AuthServices](
    f:                     I => F[api.UserID]
  )(implicit codePosition: CodePosition): AuthorizeOwnedLogic[F, I, I2, E, O, S] =
    new AuthorizeOwnedLogic[F, I, I2, E, O, S] {
      protected type Rest1 = self.Rest1
      protected type Rest2 = self.Rest2
      protected val mapped      = self.mapped
      protected val extractAuth = self.extractAuth
      protected val extractPerm = self.extractPerm
      protected val addUser     = self.addUser
      protected val ownership   = f
    }
}

abstract class AuthorizeOwnedLogic[F[_]: Sync: AuthServices, I, I2, E, O, S](implicit codePosition: CodePosition) {
  protected type Rest1
  protected type Rest2
  protected val mapped:      Endpoint[I, E, O, S]
  protected val extractAuth: I => (Rest1, api.Authentication)
  protected val extractPerm: Rest1 => (Rest2, api.RequiredPermissions)
  protected val addUser:     (Rest2, (users.model.User, Option[users.model.Session])) => I2
  protected val ownership:   I => F[api.UserID]

  def serverLogic(f: I2 => F[Either[E, O]]): ServerEndpoint[I, E, O, S, F] =
    mapped.serverLogic[F] { i =>
      val (rest1, auth) = extractAuth(i)
      val (rest2, perm) = extractPerm(rest1)
      ownership(i)
        .handleErrorWith(_ =>
          Sync[F].raiseError(CommonError.InsufficientPermissions("Ownership was not confirmed", codePosition))
        )
        .flatMap(userID => AuthServices[F].authorizeUser(auth, perm, Some(userID)))
        .map(addUser(rest2, _))
        .flatMap(f)
    }
}
