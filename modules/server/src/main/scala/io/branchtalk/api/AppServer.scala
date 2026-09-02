package io.branchtalk.api

import cats.arrow.FunctionK
import cats.data.{ Kleisli, NonEmptyList }
import cats.effect.{ Async, Resource }
import fs2.compression.Compression
import io.branchtalk.auth.{ AuthServices, AuthServicesImpl }
import io.branchtalk.configs.{ APIConfig, APIPart, AppArguments, PaginationConfig }
import io.branchtalk.discussions.api.{ ChannelServer, CommentServer, PostServer, SearchServer, SubscriptionServer }
import io.branchtalk.discussions.reads.*
import io.branchtalk.discussions.writes.*
import io.branchtalk.logging.MDC
import io.branchtalk.notifications.api.{ NotificationServer, NotificationWebSocket }
import io.branchtalk.notifications.reads.NotificationReads
import io.branchtalk.notifications.writes.{ NotificationTopic, NotificationWrites }
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
import org.http4s.dsl.Http4sDsl
import io.prometheus.client.exporter.common.TextFormat
import org.http4s.implicits.*
import org.http4s.metrics.MetricsOps
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.server.Server
import org.http4s.server.middleware.*
import sttp.tapir.server.ServerEndpoint

import java.util.concurrent.{ Executors, ThreadFactory }
import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

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
  searchServer:            SearchServer[F],
  notificationServer:      NotificationServer[F],
  openAPIServer:           OpenAPIServer[F],
  metricsOps:              MetricsOps[F],
  correlationIDOps:        CorrelationIDOps[F],
  requestIDOps:            RequestIDOps[F],
  idempotencyMiddleware:   Option[HttpRoutes[F] => HttpRoutes[F]],
  registry:                CollectorRegistry,
  apiConfig:               APIConfig
) {

  private val corsConfig = CORS.policy
    .pipe { policy =>
      if (apiConfig.http.corsAnyOrigin) policy.withAllowOriginAll
      else if (apiConfig.http.corsAllowedOrigins.nonEmpty) {
        val allowedSet = apiConfig.http.corsAllowedOrigins.map(_.toLowerCase(java.util.Locale.ROOT)).toSet
        policy.withAllowOriginHostCi(origin => allowedSet.contains(origin.toString.toLowerCase(java.util.Locale.ROOT)))
      } else policy
    }
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

  private val metricsDsl = new Http4sDsl[F] {}

  // Prometheus scrape endpoint. Served OUTSIDE the middleware pipeline (below) so scrapers get raw text/plain and the
  // endpoint neither measures nor gzips itself.
  private val metricsRoute: HttpRoutes[F] = {
    import metricsDsl.*
    HttpRoutes.of[F] { case GET -> Root / "metrics" =>
      Async[F].delay {
        val writer = new java.io.StringWriter()
        TextFormat.write004(writer, registry.metricFamilySamples())
        writer.toString
      }.flatMap(Ok(_))
    }
  }

  val routes: HttpApp[F] = {
    val mainRoutes: HttpRoutes[F] =
      NonEmptyList
        .of(
          // The literal /users/moderation and /users/bans routes must come before userServer's /users/{userID},
          // otherwise the path-param route matches "moderation"/"bans" and fails to parse them as a UUID (routes are
          // tried in order).
          userModerationServer.routes,
          userBanServer.routes,
          userServer.routes,
          channelModerationServer.routes,
          channelBanServer.routes,
          channelServer.routes,
          postServer.routes,
          commentServer.routes,
          subscriptionServer.routes,
          searchServer.routes,
          notificationServer.routes,
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
    (metricsRoute <+> mainRoutes).orNotFound
      .pipe(logRoutes)
  }
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
    subscriptionWrites: SubscriptionWrites[F],
    notificationReads:  NotificationReads[F],
    notificationWrites: NotificationWrites[F],
    notificationTopic:  NotificationTopic[F]
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
        val searchServer: SearchServer[F] =
          SearchServer[F](authServices, postReads, apiConfig.safePagination(APIPart.Posts))
        val notificationServer: NotificationServer[F] = NotificationServer[F](
          authServices,
          notificationReads,
          notificationWrites,
          apiConfig.safePagination(APIPart.Notifications)
        )
        val notificationWebSocket: NotificationWebSocket[F] = NotificationWebSocket[F](authServices, notificationTopic)
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
              subscriptionServer.endpoints,
              searchServer.endpoints,
              notificationServer.endpoints
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
          searchServer,
          notificationServer,
          openAPIServer,
          metricsOps,
          correlationIDOps,
          requestIDOps,
          idempotencyMiddleware,
          registry,
          apiConfig
        )

        val logger = io.branchtalk.logging.Logger.getLogger[F]

        val httpPoolSize = Runtime.getRuntime.availableProcessors().max(2)
        @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
        val httpThreadPool: Resource[F, ExecutionContext] = Resource.make {
          Async[F].delay {
            val counter = new AtomicInteger(0)
            val factory: ThreadFactory = (r: Runnable) => {
              val t = new Thread(r, s"http-pool-${counter.getAndIncrement()}")
              t.setDaemon(true)
              t
            }
            ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(httpPoolSize, factory))
          }
        }(ec => Async[F].delay(ec.asInstanceOf[java.util.concurrent.ExecutorService].shutdown()))

        Resource.make(logger.info("Starting up API server"))(_ => logger.info("API server shut down")) >>
          httpThreadPool.flatMap { httpEC =>
            BlazeServerBuilder[F]
              .withExecutionContext(httpEC)
              .withLengthLimits(maxRequestLineLen = apiConfig.http.maxRequestLineLength,
                                maxHeadersLen = apiConfig.http.maxHeaderLineLength
              )
              .bindHttp(port = appArguments.port, host = appArguments.host)
              .withHttpWebSocketApp { wsb =>
                // websocket route is kept OUTSIDE the tapir/GZip/metrics pipeline; it falls through to the REST app
                Kleisli { (req: Request[F]) =>
                  notificationWebSocket.routes(wsb).run(req).getOrElseF(appServer.routes.run(req))
                }
              }
              .resource
              .flatTap { server =>
                Resource.eval(logger.info(s"API server started at ${server.address.toString}"))
              }
          }
      } // idempotencyRedis.flatMap
    }
}
