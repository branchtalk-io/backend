package io.branchtalk.api

import cats.data.{ Kleisli, OptionT }
import cats.effect.{ Async, Resource }
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import io.branchtalk.configs.APIIdempotency
import io.branchtalk.logging.Logger
import org.http4s.*

import java.util.Base64
import scala.concurrent.duration.FiniteDuration

/** HTTP-layer idempotency middleware.
  *
  * For state-modifying methods (POST, PUT, PATCH, DELETE) that carry an `X-Request-ID` header, the middleware checks
  * Redis for a previously cached response. On a cache hit the stored response (status + body) is replayed immediately
  * with an `X-Idempotent-Replayed: true` marker header, skipping the downstream handler entirely. On a cache miss the
  * handler runs normally and its response is buffered, stored in Redis with a configurable TTL, then returned.
  *
  * GET, HEAD, and OPTIONS requests pass through unconditionally (they are safe/idempotent by definition).
  *
  * '''Key-scoping decision''': the cache key is derived from the `X-Request-ID` value alone. Because Tapir handles
  * authentication inside the endpoint logic (not at the http4s middleware layer), the authenticated principal is not
  * available here. This is acceptable: the `X-Request-ID` is a client-generated unique identifier for a single logical
  * request, so two different users should never share the same request id. Permission changes are reflected correctly
  * because the first execution still runs the full auth-guarded handler; only an identical retry (same request id) gets
  * the cached response.
  */
object IdempotencyMiddleware {

  private val requestIdHeader = org.typelevel.ci.CIString("X-Request-ID")
  private val replayedHeader  = org.typelevel.ci.CIString("X-Idempotent-Replayed")

  private val modifyingMethods: Set[Method] =
    Set(Method.POST, Method.PUT, Method.PATCH, Method.DELETE)

  // Separator between the status code and the Base64-encoded body in the cached Redis value.
  private val EntrySeparator = "|"

  private def encodeEntry(status: Status, body: Array[Byte]): String = {
    val bodyB64 = Base64.getEncoder.encodeToString(body)
    s"${status.code}$EntrySeparator$bodyB64"
  }

  private def decodeEntry(raw: String): Option[(Status, Array[Byte])] = {
    val idx = raw.indexOf(EntrySeparator)
    if (idx <= 0) None
    else {
      val code    = raw.substring(0, idx).toIntOption
      val bodyB64 = raw.substring(idx + EntrySeparator.length)
      for {
        c      <- code
        status <- Status.fromInt(c).toOption
        body    = Base64.getDecoder.decode(bodyB64)
      } yield (status, body)
    }
  }

  /** Build a Redis resource for the idempotency cache, using the same pattern as `Cache.fromConfigs`. */
  def redisResource[F[_]: Async](config: APIIdempotency): Resource[F, RedisCommands[F, String, String]] = {
    val logger = Logger.getLogger[F]
    given Log[F] = new Log[F] {
      override def debug(msg: => String): F[Unit] = logger.debug(msg)
      override def error(msg: => String): F[Unit] = logger.error(msg)
      override def info(msg:  => String): F[Unit] = logger.info(msg)
    }
    Redis[F].simple(show"redis://${config.redis}", RedisCodec.Utf8)
  }

  /** Apply the middleware to an `HttpRoutes[F]`.
    *
    * Must sit inside the route pipeline close to the application routes so that outer middleware (GZip, CORS, Metrics,
    * correlation/request-id injection) still wraps the replayed response.
    */
  def apply[F[_]: Async](
    redis: RedisCommands[F, String, String],
    ttl:   FiniteDuration
  )(routes: HttpRoutes[F]): HttpRoutes[F] = {
    val logger = Logger.getLogger[F]

    Kleisli { (req: Request[F]) =>
      if (!modifyingMethods.contains(req.method)) {
        // Safe method: pass through.
        routes(req)
      } else {
        req.headers.get(requestIdHeader) match {
          case None =>
            // No X-Request-ID: pass through without caching.
            routes(req)
          case Some(header) =>
            val requestId = header.head.value
            val cacheKey  = s"idempotency:$requestId"

            // If Redis is unreachable, fall through to the normal route rather than failing the request.
            OptionT(redis.get(cacheKey).flatMap {
              case Some(raw) =>
                // Cache hit: decode and replay.
                decodeEntry(raw) match {
                  case Some((status, body)) =>
                    Response[F](
                      status = status,
                      body = fs2.Stream.chunk(fs2.Chunk.array(body)),
                      headers = Headers(Header.Raw(replayedHeader, "true"))
                    ).some.pure[F]
                  case None =>
                    // Corrupted cache entry: fall through to execute the route normally.
                    routes(req).value
                }
              case None =>
                // Cache miss: run the route, buffer the body, store the response.
                routes(req).semiflatMap { response =>
                  response.body.compile.toVector.map(_.toArray).flatMap { bodyBytes =>
                    val encoded = encodeEntry(response.status, bodyBytes)
                    // Best-effort cache store: if it fails, still return the response.
                    redis.setEx(cacheKey, encoded, ttl)
                      .handleErrorWith(e => logger.warn(e)(s"Failed to cache idempotency response for $requestId"))
                      .as(response.copy(body = fs2.Stream.chunk(fs2.Chunk.array(bodyBytes))))
                  }
                }.value
            }.handleErrorWith { e =>
              logger.warn(e)(s"Idempotency cache lookup failed for $requestId, proceeding without cache")
                .flatMap(_ => routes(req).value)
            })
        }
      }
    }
  }
}
