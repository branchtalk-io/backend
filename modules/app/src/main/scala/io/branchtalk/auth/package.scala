package io.branchtalk

import cats.{ Applicative, Functor }
import io.branchtalk.api.{ AuthMapping, AuthMappingWithOwnership, RequiredPermissions, UserID }
import io.branchtalk.shared.models.{ TupleAppender, TuplePrepender }
import sttp.tapir.Endpoint

package object auth {

  // authorizes by mapping Authentication into (User, Option[Session])

  implicit def authMapping[F[_]: Functor: AuthServices, In, OOut, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, api.Authentication, In],
    preUser: TuplePrepender.Aux[Inter, (users.model.User, Option[users.model.Session]), OOut]
  ): AuthMapping.Aux[F, In, OOut] = new AuthMapping[F, In] {
    type Out = OOut

    override def authorize(i: In, requiredPermissions: RequiredPermissions): F[OOut] = {
      val (rest, auth) = preAuth.revert(i)
      AuthServices[F].authorizeUser(auth, requiredPermissions, None).map(preUser.prepend(rest, _))
    }
  }

  implicit def optAuthMapping[F[_]: Applicative: AuthServices, I, II2, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, Option[api.Authentication], I],
    preUser: TuplePrepender.Aux[Inter, (Option[users.model.User], Option[users.model.Session]), II2]
  ): AuthMapping.Aux[F, I, II2] = new AuthMapping[F, I] {
    type Out = II2

    override def authorize(i: I, requiredPermissions: RequiredPermissions): F[II2] = {
      val (rest, auth) = preAuth.revert(i)
      auth
        .traverse(AuthServices[F].authorizeUser(_, requiredPermissions, None))
        .map { optUserSession =>
          optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
        }
        .map(preUser.prepend(rest, _))
    }
  }

  // authorizes bu mapping Authentication into (User, Option[Session]) but also takes UserID as resource owner
  // to allow running I => F[UserID] as ownership check

  implicit def authMappingWithUserIDOwnership[F[_]: Functor: AuthServices, I, OOut, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, api.Authentication, I],
    preUser: TuplePrepender.Aux[Inter, (users.model.User, Option[users.model.Session]), OOut]
  ): AuthMappingWithOwnership.Aux[F, I, OOut, UserID] = new AuthMappingWithOwnership[F, I] {
    type Out   = OOut
    type Owner = UserID

    override def authorize(i: I, requiredPermissions: RequiredPermissions, owner: UserID): F[OOut] = {
      val (rest, auth) = preAuth.revert(i)
      AuthServices[F].authorizeUser(auth, requiredPermissions, owner.some).map(preUser.prepend(rest, _))
    }
  }

  implicit def optAuthMappingWithUserIDOwnership[F[_]: Applicative: AuthServices, I, II2, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, Option[api.Authentication], I],
    preUser: TuplePrepender.Aux[Inter, (Option[users.model.User], Option[users.model.Session]), II2]
  ): AuthMappingWithOwnership.Aux[F, I, II2, UserID] = new AuthMappingWithOwnership[F, I] {
    type Out   = II2
    type Owner = UserID

    override def authorize(i: I, requiredPermissions: RequiredPermissions, owner: UserID): F[II2] = {
      val (rest, auth) = preAuth.revert(i)
      auth
        .traverse(AuthServices[F].authorizeUser(_, requiredPermissions, owner.some))
        .map { optUserSession =>
          optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
        }
        .map(preUser.prepend(rest, _))
    }
  }

  // authorizes bu mapping Authentication into (User, Option[Session]) but also takes Unit as resource owner
  // to allow running I => F[Unit] as ownership check

  implicit def authMappingWithUnitOwnership[F[_]: Functor: AuthServices, I, OOut, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, api.Authentication, I],
    preUser: TuplePrepender.Aux[Inter, (users.model.User, Option[users.model.Session]), OOut]
  ): AuthMappingWithOwnership.Aux[F, I, OOut, Unit] = new AuthMappingWithOwnership[F, I] {
    type Out   = OOut
    type Owner = Unit

    override def authorize(i: I, requiredPermissions: RequiredPermissions, owner: Unit): F[OOut] = {
      val (rest, auth) = preAuth.revert(i)
      AuthServices[F].authorizeUser(auth, requiredPermissions, None).map(preUser.prepend(rest, _))
    }
  }

  implicit def optAuthMappingWithUnitOwnership[F[_]: Applicative: AuthServices, I, II2, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, Option[api.Authentication], I],
    preUser: TuplePrepender.Aux[Inter, (Option[users.model.User], Option[users.model.Session]), II2]
  ): AuthMappingWithOwnership.Aux[F, I, II2, Unit] = new AuthMappingWithOwnership[F, I] {
    type Out   = II2
    type Owner = Unit

    override def authorize(i: I, requiredPermissions: RequiredPermissions, owner: Unit): F[II2] = {
      val (rest, auth) = preAuth.revert(i)
      auth
        .traverse(AuthServices[F].authorizeUser(_, requiredPermissions, None))
        .map { optUserSession =>
          optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
        }
        .map(preUser.prepend(rest, _))
    }
  }

  // TODO: remove after replacing
  implicit class AuthOps[I, E, O](private val endpoint: Endpoint[I, E, O, Nothing]) extends AnyVal {

    def authenticated[Inter, I2](implicit
      preAuth: TuplePrepender.Aux[Inter, api.Authentication, I],
      preUser: TuplePrepender.Aux[Inter, (users.model.User, Option[users.model.Session]), I2]
    ): AuthenticateLogic[I, I2, E, O, Nothing] = new AuthenticateLogic[I, I2, E, O, Nothing] {
      protected type Rest = Inter
      protected val mapped      = endpoint
      protected val extractAuth = preAuth.revert(_)
      protected val addUser     = preUser.prepend(_, _)
    }

    def optAuthenticated[Inter, I2](implicit
      preAuth: TuplePrepender.Aux[Inter, Option[api.Authentication], I],
      preUser: TuplePrepender.Aux[Inter, (Option[users.model.User], Option[users.model.Session]), I2]
    ): OptAuthenticateLogic[I, I2, E, O, Nothing] = new OptAuthenticateLogic[I, I2, E, O, Nothing] {
      protected type Rest = Inter
      protected val mapped      = endpoint
      protected val extractAuth = preAuth.revert(_)
      protected val addUser     = preUser.prepend(_, _)
    }

    def authorized[Inter1, Inter2, I2](implicit
      preAuth: TuplePrepender.Aux[Inter1, api.Authentication, I],
      appPerm: TupleAppender.Aux[Inter2, api.RequiredPermissions, Inter1],
      preUser: TuplePrepender.Aux[Inter2, (users.model.User, Option[users.model.Session]), I2]
    ): AuthorizeLogic[I, I2, E, O, Nothing] = new AuthorizeLogic[I, I2, E, O, Nothing] {
      protected type Rest1 = Inter1
      protected type Rest2 = Inter2
      protected val mapped      = endpoint
      protected val extractAuth = preAuth.revert(_)
      protected val extractPerm = appPerm.revert(_)
      protected val addUser     = preUser.prepend(_, _)
    }
  }
}
