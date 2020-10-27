package io.branchtalk.api

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Sync, Timer }
import com.softwaremill.macwire.wire
import io.branchtalk.configs.{ APIConfig, APIPart, AppConfig, PaginationConfig }
import io.branchtalk.discussions.api.PostServer
import io.branchtalk.discussions.{ DiscussionsReads, DiscussionsWrites }
import io.branchtalk.openapi.OpenAPIServer
import io.branchtalk.users.api.UserServer
import io.branchtalk.users.{ UsersReads, UsersWrites }
import io.branchtalk.users.services.{ AuthServices, AuthServicesImpl }
import org.http4s._
import org.http4s.implicits._
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.metrics.MetricsOps
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Server
import sttp.tapir.server.ServerEndpoint
import org.http4s.server.middleware._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

final class AppServer[F[_]: Sync: Timer](
  usesServer:    UserServer[F],
  postServer:    PostServer[F],
  openAPIServer: OpenAPIServer[F],
  metricsOps:    MetricsOps[F]
) {

  // TODO: pass configuration
  private val corsConfig = CORSConfig(anyOrigin = true, allowCredentials = true, maxAge = 1.day.toSeconds)

  val routes: HttpApp[F] =
    NonEmptyList
      .of(usesServer.routes, postServer.routes, openAPIServer.routes)
      .reduceK
      .pipe(GZip(_))
      .pipe(CORS(_, corsConfig))
      .pipe(Metrics[F](metricsOps))
      .orNotFound
}
object AppServer {

  private def prometheusMetrics[F[_]: Sync]: Resource[F, MetricsOps[F]] =
    for {
      registry <- Prometheus.collectorRegistry[F]
      metrics <- Prometheus.metricsOps[F](registry, "server")
    } yield metrics

  def asResource[F[_]: ConcurrentEffect: ContextShift: Timer](
    appConfig:         AppConfig,
    apiConfig:         APIConfig,
    usersReads:        UsersReads[F],
    usersWrites:       UsersWrites[F],
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): Resource[F, Server[F]] = prometheusMetrics[F].flatMap { metricsOps =>
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
      val endpoints: NonEmptyList[ServerEndpoint[_, _, _, Nothing, F]] =
        NonEmptyList.of(usersServer.endpoints, postServer.endpoints).reduceK
      wire[OpenAPIServer[F]]
    }

    val appServer = wire[AppServer[F]]

    BlazeServerBuilder[F](ExecutionContext.global) // TODO: make configurable
      .enableHttp2(apiConfig.http.http2Enabled)
      .withLengthLimits(maxRequestLineLen = apiConfig.http.maxRequestLineLength.value,
                        maxHeadersLen     = apiConfig.http.maxHeaderLineLength.value)
      .bindHttp(port = appConfig.port, host = appConfig.host)
      .withHttpApp(appServer.routes)
      .resource
  }
}
