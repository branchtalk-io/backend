package io.branchtalk.api

import cats.effect.{ Resource, Sync, Timer }
import io.branchtalk.configs.{ APIConfig, APIContact, APIHttp, APIInfo, APILicense, AppArguments }
import io.branchtalk.shared.models.UUIDGenerator

import scala.collection.mutable
import scala.concurrent.duration._

object TestApiConfigs {

  private val allowedPorts = (23456 to 24000).toSet // TODO: figure out how to obtain it better
  private val takenPorts   = mutable.Set.empty[Int]

  private def acquirePort[F[_]: Sync: Timer]: F[Int] = ().tailRecM[F, Int] { _ =>
    Sync[F].defer {
      synchronized {
        (allowedPorts -- takenPorts).toList match {
          case free :: _ => free.tap(takenPorts.add).asRight[Unit].pure[F]
          case _         => Timer[F].sleep(250.millis).as(().asLeft[Int])
        }
      }
    }
  }

  private def releasePort[F[_]: Sync](port: Int): F[Unit] = Sync[F].delay {
    synchronized(takenPorts.remove(port))
    ()
  }

  private def portResource[F[_]: Sync: Timer]: Resource[F, Int] = Resource.make(acquirePort[F])(releasePort[F](_))

  def asResource[F[_]: Sync: Timer](implicit UUIDGenerator: UUIDGenerator): Resource[F, (AppArguments, APIConfig)] =
    (Resource.liftF(UUIDGenerator.create[F]), portResource[F]).mapN { (defaultChannelID, port) =>
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
          termsOfService = "http://branchtalk.io",
          contact = APIContact(name = "test", email = "test@brachtalk.io", url = "http://branchtalk.io"),
          license = APILicense(name = "test", url = "http://branchtalk.io")
        ),
        http = APIHttp(
          logHeaders = true,
          logBody = true,
          http2Enabled = true,
          corsAnyOrigin = true,
          corsAllowCredentials = true,
          corsMaxAge = 1.day,
          maxHeaderLineLength = 512,
          maxRequestLineLength = 1024
        ),
        defaultChannels = List(defaultChannelID),
        pagination = Map.empty
      )
      app -> api
    }
}
