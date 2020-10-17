package io.branchtalk.api

import cats.data.{ Kleisli, NonEmptyList }
import cats.effect.Sync
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.users.api.UserServer
import org.http4s.implicits._
import org.http4s.{ Request, Response }

final class AppServer[F[_]: Sync](
  usesServer:    UserServer[F],
  postServer:    PostServer[F],
  openAPIServer: OpenAPIServer[F]
) {

  // TODO: proper auth service with would maybe as middleware, it should check required permissions before running F
  // TODO: CORS service (HTTP4s has a middleware for it)
  // TODO: Prometheus (HTTP4s has a middleware for it as well)
  val routes: Kleisli[F, Request[F], Response[F]] =
    NonEmptyList.of(usesServer.userRoutes, postServer.postRoutes, openAPIServer.openAPIRoutes).reduceK.orNotFound
}
