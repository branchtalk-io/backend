package io.branchtalk.api

import cats.{ Functor, Monad, MonadError }
import io.branchtalk.shared.model.{ CodePosition, CommonError }
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

trait Authorize[F[_], Auth, Out] {

  def authorize(auth: Auth, requiredPermissions: RequiredPermissions): F[Out]
}
object Authorize {

  implicit def functor[F[_]: Functor, Auth]: Functor[Authorize[F, Auth, *]] = new Functor[Authorize[F, Auth, *]] {
    override def map[A, B](fa: Authorize[F, Auth, A])(f: A => B): Authorize[F, Auth, B] =
      (auth: Auth, requiredPermissions: RequiredPermissions) => fa.authorize(auth, requiredPermissions).map(f)
  }
}

trait AuthorizeWithOwnership[F[_], Auth, Owner, Out] {

  def authorize(auth: Auth, requiredPermissions: RequiredPermissions, owner: Owner): F[Out]
}
object AuthorizeWithOwnership {

  implicit def functor[F[_]: Functor, Auth, Owner]: Functor[AuthorizeWithOwnership[F, Auth, Owner, *]] =
    new Functor[AuthorizeWithOwnership[F, Auth, Owner, *]] {
      override def map[A, B](
        fa: AuthorizeWithOwnership[F, Auth, Owner, A]
      )(f:  A => B): AuthorizeWithOwnership[F, Auth, Owner, B] =
        (auth: Auth, requiredPermissions: RequiredPermissions, owner: Owner) =>
          fa.authorize(auth, requiredPermissions, owner).map(f)
    }
}

// in practice we just unpack value of the header and pass it to services
final case class AuthedEndpoint[A, I, E, O, R](
  endpoint:        Endpoint[A, I, E, O, R],
  makePermissions: I => RequiredPermissions
) {

  def serverLogic[F[_], U]: AuthedEndpoint.PartialServerLogic[F, U, A, I, E, O, R] =
    new AuthedEndpoint.PartialServerLogic(endpoint, makePermissions)

  def serverLogicWithOwnership[F[_], U, Owner](
    ownership: I => F[Owner]
  ): AuthedEndpoint.PartialServerLogicWithOwnership[F, U, A, I, E, O, R, Owner] =
    new AuthedEndpoint.PartialServerLogicWithOwnership(endpoint, makePermissions, ownership)
}
object AuthedEndpoint {

  final class PartialServerLogic[F[_], U, A, I, E, O, R](
    endpoint:        Endpoint[A, I, E, O, R],
    makePermissions: I => RequiredPermissions
  ) {
    def apply(
      logic: I => F[O]
    )(implicit
      F:            Monad[F],
      errorHandler: ServerErrorHandler[F, E],
      authorize:    Authorize[F, A, U]
    ): ServerEndpoint.Full[A, A, I, E, O, R, F] =
      buildServerEndpoint((i, _, _) => i, logic)

    def withUser(
      logic: (U, I) => F[O]
    )(implicit
      F:            Monad[F],
      errorHandler: ServerErrorHandler[F, E],
      authorize:    Authorize[F, A, U]
    ): ServerEndpoint.Full[A, A, I, E, O, R, F] =
      buildServerEndpoint((i, _, u) => (u, i), logic.tupled)

    def justUser(
      logic: U => F[O]
    )(implicit
      F:            Monad[F],
      errorHandler: ServerErrorHandler[F, E],
      authorize:    Authorize[F, A, U],
      ev:           I =:= Unit
    ): ServerEndpoint.Full[A, A, I, E, O, R, F] =
      buildServerEndpoint((_, _, u) => u, logic)

    private def buildServerEndpoint[In](
      input: (I, A, U) => In,
      logic: In => F[O]
    )(implicit
      F:            Monad[F],
      errorHandler: ServerErrorHandler[F, E],
      authorize:    Authorize[F, A, U]
    ): ServerEndpoint.Full[A, A, I, E, O, R, F] =
      endpoint.serverSecurityLogicPure(_.asRight[E]).serverLogic { auth: A => i: I =>
        for {
          u <- authorize.authorize(auth, makePermissions(i))
          in = input(i, auth, u)
          out <- errorHandler(logic(in))
        } yield out
      }
  }

  final class PartialServerLogicWithOwnership[F[_], U, A, I, E, O, R, Owner](
    endpoint:        Endpoint[A, I, E, O, R],
    makePermissions: I => RequiredPermissions,
    ownership:       I => F[Owner]
  ) {
    def apply(
      logic: I => F[O]
    )(implicit
      F:            MonadError[F, Throwable],
      errorHandler: ServerErrorHandler[F, E],
      authorize:    AuthorizeWithOwnership[F, A, Owner, U],
      codePosition: CodePosition
    ): ServerEndpoint.Full[A, A, I, E, O, R, F] =
      buildServerEndpoint((i, _, _) => i, logic)

    def withUser(
      logic: (U, I) => F[O]
    )(implicit
      F:            MonadError[F, Throwable],
      errorHandler: ServerErrorHandler[F, E],
      authorize:    AuthorizeWithOwnership[F, A, Owner, U],
      codePosition: CodePosition
    ): ServerEndpoint.Full[A, A, I, E, O, R, F] =
      buildServerEndpoint((i, _, u) => (u, i), logic.tupled)

    def justUser(
      logic: U => F[O]
    )(implicit
      F:            MonadError[F, Throwable],
      errorHandler: ServerErrorHandler[F, E],
      authorize:    AuthorizeWithOwnership[F, A, Owner, U],
      codePosition: CodePosition,
      ev:           I =:= Unit
    ): ServerEndpoint.Full[A, A, I, E, O, R, F] =
      buildServerEndpoint((_, _, u) => u, logic)

    private def buildServerEndpoint[In](
      input: (I, A, U) => In,
      logic: In => F[O]
    )(implicit
      F:            MonadError[F, Throwable],
      errorHandler: ServerErrorHandler[F, E],
      authorize:    AuthorizeWithOwnership[F, A, Owner, U],
      codePosition: CodePosition
    ): ServerEndpoint.Full[A, A, I, E, O, R, F] =
      endpoint.serverSecurityLogicPure(_.asRight[E]).serverLogic { auth: A => i: I =>
        for {
          owner <- ownership(i).handleErrorWith { _ =>
            (CommonError.InsufficientPermissions("Ownership was not confirmed", codePosition): Throwable)
              .raiseError[F, Owner]
          }
          u <- authorize.authorize(auth, makePermissions(i), owner)
          in = input(i, auth, u)
          out <- errorHandler(logic(in))
        } yield out
      }
  }
}
