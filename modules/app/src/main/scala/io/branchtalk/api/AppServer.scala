package io.branchtalk.api

import cats.data.{ Kleisli, NonEmptyList, OptionT }
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Sync, Timer }
import com.softwaremill.macwire.wire
import com.typesafe.scalalogging.Logger
import io.branchtalk.configs.{ APIConfig, APIPart, AppConfig, PaginationConfig }
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.discussions.{ DiscussionsReads, DiscussionsWrites }
import io.branchtalk.openapi.OpenAPIServer
import io.branchtalk.users.api.UserServer
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.branchtalk.users.services.{ AuthServices, AuthServicesImpl }
import org.http4s.implicits._
import org.http4s.{ Request, Response }
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Server

import scala.concurrent.ExecutionContext

final class AppServer[F[_]: Sync](
  usesServer:    UserServer[F],
  postServer:    PostServer[F],
  openAPIServer: OpenAPIServer[F]
) {

  private val logger = Logger(getClass)

  val routes: Kleisli[F, Request[F], Response[F]] =
    NonEmptyList
      .of(usesServer.userRoutes, postServer.postRoutes, openAPIServer.openAPIRoutes)
      .reduceK
      .local { req: Request[F] =>
        logger.info(s"Received request ${req.method.name.toUpperCase} ${req.uri}")
        req
      }
      .map { response =>
        logger.info(s"Received succeeded with ${response.status}")
        response
      }
      .handleErrorWith { error: Throwable =>
        logger.error(s"Request failed with error", error)
        Kleisli.liftF(error.raiseError[OptionT[F, *], Response[F]])
      }
      .orNotFound
}
object AppServer {

  def asResource[F[_]: ConcurrentEffect: ContextShift: Timer](
    appConfig:         AppConfig,
    apiConfig:         APIConfig,
    usersReads:        UsersReads[F],
    usersWrites:       UsersWrites[F],
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): Resource[F, Server[F]] = {
    // this is kind of silly...
    import usersReads.{ sessionReads, userReads }
    import usersWrites.{ sessionWrites, userWrites }
    import discussionsReads.{ channelReads, commentReads, postReads, subscriptionReads }
    import discussionsWrites.{ channelWrites, commentWrites, postWrites, subscriptionWrites }

    val authServices: AuthServices[F] = wire[AuthServicesImpl[F]]

    val usersServer: UserServer[F] = {
      val paginationConfig: PaginationConfig = apiConfig.safePagination(APIPart.Users)
      wire[UserServer[F]]
    }
    val postServer: PostServer[F] = {
      val paginationConfig: PaginationConfig = apiConfig.safePagination(APIPart.Posts)
      wire[PostServer[F]]
    }
    val openAPIServer: OpenAPIServer[F] = {
      import apiConfig.info
      wire[OpenAPIServer[F]]
    }

    val appServer = wire[AppServer[F]]

    BlazeServerBuilder[F](ExecutionContext.global)
      .bindHttp(port = appConfig.port, host = appConfig.host)
      .withHttpApp(appServer.routes)
      .resource
  }
}
