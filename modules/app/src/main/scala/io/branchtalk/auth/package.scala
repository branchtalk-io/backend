package io.branchtalk

import cats.{ Applicative, Functor }
import io.branchtalk.api.{ AuthMapping, AuthMappingWithOwnership, RequiredPermissions, UserID }
import io.branchtalk.shared.models.TuplePrepender

package object auth {

  // authorizes by mapping Authentication into (User, Option[Session])

  implicit def authMapping[F[_]: Functor: AuthServices, In, OOut, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, api.Authentication, In],
    preUser: TuplePrepender.Aux[Inter, (users.model.User, Option[users.model.Session]), OOut]
  ): AuthMapping.Aux[F, In, OOut] = new AuthMapping[F, In] {
    type Out = OOut

    override def authorize(in: In, requiredPermissions: RequiredPermissions): F[OOut] = {
      val (rest, auth) = preAuth.revert(in)
      AuthServices[F].authorizeUser(auth, requiredPermissions, None).map(preUser.prepend(rest, _))
    }
  }

  implicit def optAuthMapping[F[_]: Applicative: AuthServices, In, OOut, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, Option[api.Authentication], In],
    preUser: TuplePrepender.Aux[Inter, (Option[users.model.User], Option[users.model.Session]), OOut]
  ): AuthMapping.Aux[F, In, OOut] = new AuthMapping[F, In] {
    type Out = OOut

    override def authorize(in: In, requiredPermissions: RequiredPermissions): F[OOut] = {
      val (rest, auth) = preAuth.revert(in)
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

  implicit def authMappingWithUserIDOwnership[F[_]: Functor: AuthServices, In, OOut, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, api.Authentication, In],
    preUser: TuplePrepender.Aux[Inter, (users.model.User, Option[users.model.Session]), OOut]
  ): AuthMappingWithOwnership.Aux[F, In, OOut, UserID] = new AuthMappingWithOwnership[F, In] {
    type Out   = OOut
    type Owner = UserID

    override def authorize(in: In, requiredPermissions: RequiredPermissions, owner: UserID): F[OOut] = {
      val (rest, auth) = preAuth.revert(in)
      AuthServices[F].authorizeUser(auth, requiredPermissions, owner.some).map(preUser.prepend(rest, _))
    }
  }

  implicit def optAuthMappingWithUserIDOwnership[F[_]: Applicative: AuthServices, In, OOut, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, Option[api.Authentication], In],
    preUser: TuplePrepender.Aux[Inter, (Option[users.model.User], Option[users.model.Session]), OOut]
  ): AuthMappingWithOwnership.Aux[F, In, OOut, UserID] = new AuthMappingWithOwnership[F, In] {
    type Out   = OOut
    type Owner = UserID

    override def authorize(in: In, requiredPermissions: RequiredPermissions, owner: UserID): F[OOut] = {
      val (rest, auth) = preAuth.revert(in)
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

    override def authorize(in: I, requiredPermissions: RequiredPermissions, owner: Unit): F[OOut] = {
      val (rest, auth) = preAuth.revert(in)
      AuthServices[F].authorizeUser(auth, requiredPermissions, None).map(preUser.prepend(rest, _))
    }
  }

  implicit def optAuthMappingWithUnitOwnership[F[_]: Applicative: AuthServices, I, OOut, Inter](implicit
    preAuth: TuplePrepender.Aux[Inter, Option[api.Authentication], I],
    preUser: TuplePrepender.Aux[Inter, (Option[users.model.User], Option[users.model.Session]), OOut]
  ): AuthMappingWithOwnership.Aux[F, I, OOut, Unit] = new AuthMappingWithOwnership[F, I] {
    type Out   = OOut
    type Owner = Unit

    override def authorize(in: I, requiredPermissions: RequiredPermissions, owner: Unit): F[OOut] = {
      val (rest, auth) = preAuth.revert(in)
      auth
        .traverse(AuthServices[F].authorizeUser(_, requiredPermissions, None))
        .map { optUserSession =>
          optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
        }
        .map(preUser.prepend(rest, _))
    }
  }
}
