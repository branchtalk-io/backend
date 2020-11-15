package io.branchtalk.api

import cats.{ Monad, MonadError }
import io.branchtalk.shared.models.{ CodePosition, CommonError }
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

// TODO: provide instances in auth

// scalastyle:off structural.type
trait AuthMapping[F[_], In] {
  type Out
  def authorize(i: In, requiredPermissions: RequiredPermissions): F[Out]
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
  def authorize(i: In, requiredPermissions: RequiredPermissions, owner: Owner): F[Out]
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
// scalastyle:on structural.type

// TODO: conversion into RichEndpoint

final case class RichEndpoint[I, E, O, -R](
  endpoint:        Endpoint[I, E, O, R],
  makePermissions: I => RequiredPermissions
) {

  def serverLogic[F[_]: Monad, I2: AuthMapping.Aux[F, I, *]](
    logic: I2 => F[Either[E, O]]
  ): ServerEndpoint[I, E, O, R, F] =
    endpoint.serverLogic { i =>
      AuthMapping[F, I].authorize(i, makePermissions(i)).flatMap(logic)
    }

  def serverLogic[F[_]: MonadError[*[_], Throwable], Owner, I2: AuthMappingWithOwnership.Aux[F, I, *, Owner]](
    ownership: I => F[Owner]
  )(logic:     I2 => F[Either[E, O]])(implicit codePosition: CodePosition): ServerEndpoint[I, E, O, R, F] =
    endpoint.serverLogic { i =>
      ownership(i)
        .handleErrorWith(_ =>
          (CommonError.InsufficientPermissions("Ownership was not confirmed", codePosition): Throwable)
            .raiseError[F, Owner]
        )
        .flatMap { owner =>
          AuthMappingWithOwnership[F, I].authorize(i, makePermissions(i), owner).flatMap(logic)
        }
    }
}

// TODO: change all R from Nothing to Any
