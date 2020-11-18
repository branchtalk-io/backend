package io.branchtalk.api

import cats.{ Monad, MonadError }
import io.branchtalk.shared.model.{ CodePosition, CommonError }
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

// scalastyle:off structural.type
trait AuthMapping[F[_], In] {
  type Out
  def authorize(in: In, requiredPermissions: RequiredPermissions): F[Out]
}
object AuthMapping {
  def apply[F[_], In](implicit authMapping: AuthMapping[F, In]): AuthMapping[F, In] {
    type Out = authMapping.Out
  } = authMapping

  type Aux[F[_], In, OOut] = AuthMapping[F, In] {
    type Out = OOut
  }
}

trait AuthMappingWithOwnership[F[_], In] {
  type Out
  type Owner
  def authorize(in: In, requiredPermissions: RequiredPermissions, owner: Owner): F[Out]
}
object AuthMappingWithOwnership {
  def apply[F[_], In](implicit authMapping: AuthMappingWithOwnership[F, In]): AuthMappingWithOwnership[F, In] {
    type Out   = authMapping.Out
    type Owner = authMapping.Owner
  } = authMapping

  type Aux[F[_], In, OOut, OOwner] = AuthMappingWithOwnership[F, In] {
    type Out   = OOut
    type Owner = OOwner
  }
}

final case class AuthedEndpoint[I, E, O, -R](
  endpoint:        Endpoint[I, E, O, R],
  makePermissions: I => RequiredPermissions
) {

  def serverLogic[F[_]: Monad](implicit
    auth: AuthMapping[F, I]
  ): (auth.Out => F[Either[E, O]]) => ServerEndpoint[I, E, O, R, F] =
    logic =>
      endpoint.serverLogic { i =>
        auth.authorize(i, makePermissions(i)).flatMap(logic)
      }

  def serverLogicWithOwnership[F[_]: MonadError[*[_], Throwable], OOwner](implicit
    auth:         AuthMappingWithOwnership[F, I] { type Owner = OOwner },
    codePosition: CodePosition
  ): (I => F[auth.Owner]) => (auth.Out => F[Either[E, O]]) => ServerEndpoint[I, E, O, R, F] =
    ownership =>
      logic =>
        endpoint.serverLogic { i =>
          ownership(i)
            .handleErrorWith(_ =>
              (CommonError.InsufficientPermissions("Ownership was not confirmed", codePosition): Throwable)
                .raiseError[F, auth.Owner]
            )
            .flatMap { owner =>
              auth.authorize(i, makePermissions(i), owner).flatMap(logic)
            }
        }
}
// scalastyle:on structural.type
