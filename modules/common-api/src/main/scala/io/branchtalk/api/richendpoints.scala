package io.branchtalk.api

import cats.Monad
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

// TODO: provide instances in auth

// scalastyle:off structural.type
trait AuthMapping[F[_], I] {
  type I2
  def authenticate(i: I, requiredPermissions: RequiredPermissions): F[I2]
}
object AuthMapping {
  def apply[F[_], I](implicit authMapping: AuthMapping[F, I]): AuthMapping[F, I] {
    type I2 = authMapping.I2
  } = authMapping

  type Aux[F[_], I, II2] = AuthMapping[F, I] {
    type I2 = II2
  }
}

trait AuthMappingWithOwnership[F[_], I] {
  type I2
  type Owner
  def authenticate(i: I, requiredPermissions: RequiredPermissions, owner: Owner): F[I2]
}
object AuthMappingWithOwnership {
  def apply[F[_], I](implicit authMapping: AuthMappingWithOwnership[F, I]): AuthMappingWithOwnership[F, I] {
    type I2    = authMapping.I2
    type Owner = authMapping.Owner
  } = authMapping

  type Aux[F[_], I, II2, OOwner] = AuthMappingWithOwnership[F, I] {
    type I2    = II2
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
      AuthMapping[F, I].authenticate(i, makePermissions(i)).flatMap(logic)
    }

  def serverLogic[F[_]: Monad, Owner, I2: AuthMappingWithOwnership.Aux[F, I, *, Owner]](
    ownership: I => F[Owner]
  )(logic:     I2 => F[Either[E, O]]): ServerEndpoint[I, E, O, R, F] =
    endpoint.serverLogic { i =>
      ownership(i).flatMap { owner =>
        AuthMappingWithOwnership[F, I].authenticate(i, makePermissions(i), owner).flatMap(logic)
      }
    }
}
