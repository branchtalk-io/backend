package io.branchtalk.api

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.effect.{ Async, Resource }
import fs2.compression.Compression
import io.branchtalk.auth.{ AuthServices, AuthServicesImpl }
import io.branchtalk.configs.{ APIConfig, APIPart, AppArguments, PaginationConfig }
import io.branchtalk.discussions.api.{ ChannelServer, CommentServer, PostServer, SubscriptionServer }
import io.branchtalk.discussions.reads.*
import io.branchtalk.discussions.writes.*
import io.branchtalk.logging.MDC
import io.branchtalk.openapi.OpenAPIServer
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.api.{
  ChannelBanServer,
  ChannelModerationServer,
  UserBanServer,
  UserModerationServer,
  UserServer
}
import io.branchtalk.users.reads.*
import io.branchtalk.users.writes.*
import io.prometheus.client.CollectorRegistry
import dev.profunktor.redis4cats.RedisCommands
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.*
import org.http4s.metrics.MetricsOps
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.server.Server
import org.http4s.server.middleware.*
import sttp.tapir.server.ServerEndpoint

import scala.annotation.nowarn

final class AppServer[F[_]: Async: MDC](
  userServer:              UserServer[F],
  userModerationServer:    UserModerationServer[F],
  channelModerationServer: ChannelModerationServer[F],
  userBanServer:           UserBanServer[F],
  channelBanServer:        ChannelBanServer[F],
  channelServer:           ChannelServer[F],
  postServer:              PostServer[F],
  commentServer:           CommentServer[F],
  subscriptionServer:      SubscriptionServer[F],
  openAPIServer:           OpenAPIServer[F],
  metricsOps:              MetricsOps[F],
  correlationIDOps:        CorrelationIDOps[F],
  requestIDOps:            RequestIDOps[F],
  idempotencyMiddleware:   Option[HttpRoutes[F] => HttpRoutes[F]],
  apiConfig:               APIConfig
) {

  private val corsConfig = CORS.policy
    .pipe(if (apiConfig.http.corsAnyOrigin) _.withAllowOriginAll else identity)
    .withAllowCredentials(apiConfig.http.corsAllowCredentials)
    .withMaxAge(apiConfig.http.corsMaxAge)

  private val logger = io.branchtalk.logging.Logger.getLogger[F]

  private val logRoutes = Logger[F, F](
    logHeaders = apiConfig.http.logHeaders,
    logBody = apiConfig.http.logBody,
    fk = FunctionK.id,
    logAction = ((s: String) => logger.info(s)).some
  )(_)

  private given Compression[F] = Compression.forSync[F]

  val routes: HttpApp[F] =
    NonEmptyList
      .of(
        // The literal /users/moderation and /users/bans routes must come before userServer's /users/{userID}, otherwise
        // the path-param route matches "moderation"/"bans" and fails to parse them as a UUID (routes are tried in order).
        userModerationServer.routes,
        userBanServer.routes,
        userServer.routes,
        channelModerationServer.routes,
        channelBanServer.routes,
        channelServer.routes,
        postServer.routes,
        commentServer.routes,
        subscriptionServer.routes,
        openAPIServer.routes
      )
      .reduceK
      // Idempotency sits closest to the app routes: outer layers (GZip, CORS, Metrics, correlation/request-id) still
      // wrap the replayed response, so it picks up the same encoding and headers as a fresh response would.
      .pipe(r => idempotencyMiddleware.fold(r)(_(r)))
      .pipe(GZip(_))
      .pipe(corsConfig(_))
      .pipe(Metrics[F](metricsOps))
      .pipe(correlationIDOps.httpRoutes)
      .pipe(requestIDOps.httpRoutes)
      .orNotFound
      .pipe(logRoutes)
}
object AppServer {

  @SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext")) // for BlazeServer
  def asResource[F[_]: Async: MDC](
    appArguments:       AppArguments,
    apiConfig:          APIConfig,
    registry:           CollectorRegistry,
    userReads:          UserReads[F],
    sessionReads:       SessionReads[F],
    banReads:           BanReads[F],
    userWrites:         UserWrites[F],
    sessionWrites:      SessionWrites[F],
    banWrites:          BanWrites[F],
    channelReads:       ChannelReads[F],
    postReads:          PostReads[F],
    commentReads:       CommentReads[F],
    subscriptionReads:  SubscriptionReads[F],
    commentWrites:      CommentWrites[F],
    postWrites:         PostWrites[F],
    channelWrites:      ChannelWrites[F],
    subscriptionWrites: SubscriptionWrites[F]
  )(using UUID.Generator): Resource[F, Server] =
    Prometheus.metricsOps[F](registry, "server").flatMap { metricsOps =>
      // When idempotency is enabled, create a Redis connection for the response cache.
      val idempotencyRedis: Resource[F, Option[RedisCommands[F, String, String]]] =
        if (apiConfig.idempotency.enabled) IdempotencyMiddleware.redisResource[F](apiConfig.idempotency).map(_.some)
        else Resource.pure(none)

      idempotencyRedis.flatMap { redisOpt =>
      val idempotencyMiddleware: Option[HttpRoutes[F] => HttpRoutes[F]] =
        redisOpt.map[HttpRoutes[F] => HttpRoutes[F]](redis =>
          routes => IdempotencyMiddleware(redis, apiConfig.idempotency.ttl)(routes)
        )

      val correlationIDOps: CorrelationIDOps[F] = CorrelationIDOps[F]

      val requestIDOps: RequestIDOps[F] = RequestIDOps[F]

      val authServices: AuthServices[F] = AuthServicesImpl[F](userReads, sessionReads, banReads)

      val userServer: UserServer[F] = UserServer[F](
        authServices,
        userReads,
        sessionReads,
        userWrites,
        sessionWrites,
        apiConfig.safePagination(APIPart.Users)
      )
      val userModerationServer: UserModerationServer[F] =
        UserModerationServer[F](authServices, userReads, userWrites, apiConfig.safePagination(APIPart.Users))
      val channelModerationServer: ChannelModerationServer[F] =
        ChannelModerationServer[F](authServices, userReads, userWrites, apiConfig.safePagination(APIPart.Users))
      val userBanServer:    UserBanServer[F]    = UserBanServer[F](authServices, banReads, banWrites)
      val channelBanServer: ChannelBanServer[F] = ChannelBanServer[F](authServices, banReads, banWrites)
      val channelServer: ChannelServer[F] =
        ChannelServer[F](authServices, channelReads, channelWrites, apiConfig.safePagination(APIPart.Channels))
      val postServer: PostServer[F] =
        PostServer[F](authServices, postReads, postWrites, apiConfig.safePagination(APIPart.Posts))
      val commentServer: CommentServer[F] = CommentServer[F](
        authServices,
        postReads,
        commentReads,
        commentWrites,
        apiConfig.safePagination(APIPart.Comments)
      )
      val subscriptionServer: SubscriptionServer[F] = SubscriptionServer[F](
        authServices,
        postReads,
        subscriptionReads,
        subscriptionWrites,
        apiConfig,
        apiConfig.safePagination(APIPart.Posts)
      )
      val openAPIServer: OpenAPIServer[F] = OpenAPIServer[F](
        apiConfig.info,
        NonEmptyList
          .of(
            userServer.endpoints,
            userModerationServer.endpoints,
            channelModerationServer.endpoints,
            userBanServer.endpoints,
            channelBanServer.endpoints,
            channelServer.endpoints,
            postServer.endpoints,
            commentServer.endpoints,
            subscriptionServer.endpoints
          )
          .reduceK
      )

      val appServer = AppServer[F](
        userServer,
        userModerationServer,
        channelModerationServer,
        userBanServer,
        channelBanServer,
        channelServer,
        postServer,
        commentServer,
        subscriptionServer,
        openAPIServer,
        metricsOps,
        correlationIDOps,
        requestIDOps,
        idempotencyMiddleware,
        apiConfig
      )

      val logger = io.branchtalk.logging.Logger.getLogger[F]

      Resource.make(logger.info("Starting up API server"))(_ => logger.info("API server shut down")) >>
        BlazeServerBuilder[F]
          .withLengthLimits(maxRequestLineLen = apiConfig.http.maxRequestLineLength,
                            maxHeadersLen = apiConfig.http.maxHeaderLineLength
          )
          .bindHttp(port = appArguments.port, host = appArguments.host)
          .withHttpApp(appServer.routes)
          .resource
          .flatTap { server =>
            Resource.eval(logger.info(s"API server started at ${server.address.toString}"))
          }
      } // idempotencyRedis.flatMap
    }
}
