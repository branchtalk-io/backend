package io.branchtalk.api

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.effect.{ Concurrent, ConcurrentEffect, ContextShift, Resource, Sync, Timer }
import com.softwaremill.macwire.wire
import io.branchtalk.auth.{ AuthServices, AuthServicesImpl }
import io.branchtalk.configs.{ APIConfig, APIPart, AppArguments, PaginationConfig }
import io.branchtalk.discussions.api.{ PostServer, SubscriptionServer }
import io.branchtalk.discussions.{ DiscussionsReads, DiscussionsWrites }
import io.branchtalk.openapi.OpenAPIServer
import io.branchtalk.users.api.UserServer
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.prometheus.client.CollectorRegistry
import org.http4s._
import org.http4s.implicits._
import org.http4s.metrics.MetricsOps
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware._
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.ExecutionContext

final class AppServer[F[_]: Concurrent: Timer](
  usesServer:         UserServer[F],
  postServer:         PostServer[F],
  subscriptionServer: SubscriptionServer[F],
  openAPIServer:      OpenAPIServer[F],
  metricsOps:         MetricsOps[F],
  apiConfig:          APIConfig
) {

  private val logger = com.typesafe.scalalogging.Logger(getClass)

  // TODO: X-Request-ID, then cache X-Request-ID to make it idempotent
  val routes: HttpApp[F] =
    NonEmptyList
      .of(usesServer.routes, postServer.routes, subscriptionServer.routes, openAPIServer.routes)
      .reduceK
      .pipe(GZip(_))
      .pipe(
        CORS(
          _,
          CORSConfig(
            anyOrigin = apiConfig.http.corsAnyOrigin,
            allowCredentials = apiConfig.http.corsAllowCredentials,
            maxAge = apiConfig.http.corsMaxAge.toSeconds
          )
        )
      )
      .pipe(Metrics[F](metricsOps))
      .orNotFound
      .pipe(
        Logger[F, F](
          logHeaders = apiConfig.http.logHeaders,
          logBody = apiConfig.http.logBody,
          fk = FunctionK.id,
          logAction = ((s: String) => Sync[F].delay(logger.info(s))).some
        )(_)
      )
}
object AppServer {

  // scalastyle:off method.length
  def asResource[F[_]: ConcurrentEffect: ContextShift: Timer](
    appArguments:      AppArguments,
    apiConfig:         APIConfig,
    registry:          CollectorRegistry,
    usersReads:        UsersReads[F],
    usersWrites:       UsersWrites[F],
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): Resource[F, Server[F]] = Prometheus.metricsOps[F](registry, "server").flatMap { metricsOps =>
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
    val subscriptionServer: SubscriptionServer[F] = {
      val paginationConfig: PaginationConfig = apiConfig.safePagination(APIPart.Posts)
      wire[SubscriptionServer[F]]
    }
    val openAPIServer: OpenAPIServer[F] = {
      import apiConfig.info
      val endpoints: NonEmptyList[ServerEndpoint[_, _, _, Nothing, F]] =
        NonEmptyList.of(usersServer.endpoints, postServer.endpoints, subscriptionServer.endpoints).reduceK
      wire[OpenAPIServer[F]]
    }

    val appServer = wire[AppServer[F]]

    val logger = io.branchtalk.shared.models.Logger.getLogger[F]

    Resource.make(logger.info("Starting up API server"))(_ => logger.info("API server shut down")) >>
      BlazeServerBuilder[F](ExecutionContext.global) // TODO: make configurable
        .enableHttp2(apiConfig.http.http2Enabled)
        .withLengthLimits(maxRequestLineLen = apiConfig.http.maxRequestLineLength.value,
                          maxHeadersLen = apiConfig.http.maxHeaderLineLength.value
        )
        .bindHttp(port = appArguments.port, host = appArguments.host)
        .withHttpApp(appServer.routes)
        .resource
        .flatTap { server =>
          Resource.liftF(logger.info(s"API server started at ${server.address.toString}"))
        }
  }
  // scalastyle:on method.length
}
