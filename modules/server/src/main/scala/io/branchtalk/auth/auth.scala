package io.branchtalk.auth

import cats.{ Applicative, Functor }
import io.branchtalk.api.{ Authentication, Authorize, AuthorizeWithOwnership, RequiredPermissions, UserID }
import io.branchtalk.users.model.{ Session, User }

// no owner, User and Session output
given authUserSession[F[_]: AuthServices]: Authorize[F, Authentication, (User, Option[Session])] =
  (auth: Authentication, requiredPermissions: RequiredPermissions) =>
    AuthServices[F].authorizeUser(auth, requiredPermissions, None)
given authOptUserSession[F[_]: Applicative: AuthServices]: Authorize[F,
                                                                     Option[Authentication],
                                                                     (Option[User], Option[Session])
] = (auth: Option[Authentication], requiredPermissions: RequiredPermissions) =>
  auth.traverse(AuthServices[F].authorizeUser(_, requiredPermissions, None)).map { optUserSession =>
    optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
  }
// no owner, User output
given authUser[F[_]: Functor: AuthServices]: Authorize[F, Authentication, User] =
  authUserSession[F].map(_._1)
given authOptUser[F[_]: Applicative: AuthServices]: Authorize[F, Option[Authentication], Option[User]] =
  authOptUserSession[F].map(_._1)
// UserID owner, User and Session output
given authUserSessionWithUserIDOwnership[F[_]: AuthServices]: AuthorizeWithOwnership[
  F,
  Authentication,
  UserID,
  (User, Option[Session])
] =
  (auth: Authentication, requiredPermissions: RequiredPermissions, owner: UserID) =>
    AuthServices[F].authorizeUser(auth, requiredPermissions, Some(owner))
given authOptUserSessionWithUserIDOwnership[F[_]: Applicative: AuthServices]: AuthorizeWithOwnership[
  F,
  Option[Authentication],
  UserID,
  (Option[User], Option[Session])
] =
  (auth: Option[Authentication], requiredPermissions: RequiredPermissions, owner: UserID) =>
    auth.traverse(AuthServices[F].authorizeUser(_, requiredPermissions, Some(owner))).map { optUserSession =>
      optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
    }
// UserID owner, User output
given authUserWithUserIDOwnership[F[_]: Functor: AuthServices]: AuthorizeWithOwnership[
  F,
  Authentication,
  UserID,
  User
] =
  authUserSessionWithUserIDOwnership[F].map(_._1)
given authOptUserWithUserIDOwnership[F[_]: Applicative: AuthServices]: AuthorizeWithOwnership[
  F,
  Option[Authentication],
  UserID,
  Option[User]
] =
  authOptUserSessionWithUserIDOwnership[F].map(_._1)
// Unit owner, User and Session output
given authUserSessionWithUnitOwnership[F[_]: AuthServices]: AuthorizeWithOwnership[
  F,
  Authentication,
  Unit,
  (User, Option[Session])
] =
  (auth: Authentication, requiredPermissions: RequiredPermissions, _: Unit) =>
    AuthServices[F].authorizeUser(auth, requiredPermissions, None)
implicit def authOptUserSessionWithUnitOwnership[F[_]: Applicative: AuthServices]: AuthorizeWithOwnership[
  F,
  Option[Authentication],
  Unit,
  (Option[User], Option[Session])
] =
  (auth: Option[Authentication], requiredPermissions: RequiredPermissions, _: Unit) =>
    auth.traverse(AuthServices[F].authorizeUser(_, requiredPermissions, None)).map { optUserSession =>
      optUserSession.map(_._1) -> optUserSession.flatMap(_._2)
    }
// Unit owner, User output
given authUserWithUnitOwnership[F[_]: Functor: AuthServices]: AuthorizeWithOwnership[
  F,
  Authentication,
  Unit,
  User
] =
  authUserSessionWithUnitOwnership[F].map(_._1)
given authOptUserWithUnitOwnership[F[_]: Applicative: AuthServices]: AuthorizeWithOwnership[
  F,
  Option[Authentication],
  Unit,
  Option[User]
] =
  (auth: Option[Authentication], requiredPermissions: RequiredPermissions, _: Unit) =>
    auth.traverse(AuthServices[F].authorizeUser(_, requiredPermissions, None)).map { optUserSession =>
      optUserSession.map(_._1)
    }
