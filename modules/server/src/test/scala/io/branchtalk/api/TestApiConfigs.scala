package io.branchtalk.api

import java.net.URI
import cats.effect.{ Async, Resource, Sync }
import io.branchtalk.configs.{ APIConfig, APIContact, APIHttp, APIIdempotency, APIInfo, APILicense, AppArguments }
import io.branchtalk.shared.infrastructure.Server
import io.branchtalk.discussions.model.Post
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.model.User

import scala.collection.mutable
import scala.concurrent.duration.*

object TestApiConfigs {

  private val allowedPorts = (23456 to 24000).toSet // TODO: figure out how to obtain it better
  private val takenPorts   = mutable.Set.empty[Int]

  private def acquirePort[F[_]: Async]: F[Int] = ().tailRecM[F, Int] { _ =>
    Sync[F].defer {
      synchronized {
        (allowedPorts -- takenPorts).toList match {
          case free :: _ => free.tap(takenPorts.add).asRight[Unit].pure[F]
          case _         => Async[F].sleep(250.millis).as(().asLeft[Int])
        }
      }
    }
  }

  private def releasePort[F[_]: Sync](port: Int): F[Unit] = Sync[F].delay {
    synchronized(takenPorts.remove(port))
    ()
  }

  private def portResource[F[_]: Async]: Resource[F, Int] = Resource.make(acquirePort[F])(releasePort[F](_))

  def asResource[F[_]: Async](using uuidGenerator: UUID.Generator): Resource[F, (AppArguments, APIConfig)] =
    (Resource.eval(uuidGenerator.create[F]), portResource[F]).mapN { (defaultChannelID, port) =>
      val host = "localhost"
      val app = AppArguments(
        host = host,
        port = port,
        runAPI = true,
        runUsersProjections = true,
        runDiscussionsProjections = true
      )
      val api = APIConfig(
        info = APIInfo(
          title = "test",
          version = "test",
          description = "test",
          termsOfService = Post.URL.unsafeMake(URI.create("http://branchtalk.io")),
          contact = APIContact(
            name = "test",
            email = User.Email.unsafeMake("test@brachtalk.io"),
            url = Post.URL.unsafeMake(URI.create("http://branchtalk.io"))
          ),
          license = APILicense(name = "test", url = Post.URL.unsafeMake(URI.create("http://branchtalk.io")))
        ),
        http = APIHttp(
          logHeaders = true,
          logBody = true,
          corsAnyOrigin = true,
          corsAllowCredentials = true,
          corsMaxAge = 1.day,
          maxHeaderLineLength = 512,
          maxRequestLineLength = 1024
        ),
        // Disabled in tests: no Redis instance is guaranteed in the test harness, and the middleware is not under test.
        idempotency = APIIdempotency(
          enabled = false,
          ttl = 5.minutes,
          redis = Server(Server.Host.unsafeMake("127.0.0.1"), Server.Port.unsafeMake(6379))
        ),
        defaultChannels = List(defaultChannelID),
        pagination = Map.empty
      )
      app -> api
    }
}
