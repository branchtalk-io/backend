package io.branchtalk.api

import cats.effect.{ Resource, Sync, Timer }
import io.branchtalk.configs.{ APIConfig, APIContact, APIInfo, APILicense, AppConfig }

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

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

  def asResource[F[_]: Sync: Timer]: Resource[F, (AppConfig, APIConfig)] = portResource[F].map { port =>
    val host = "localhost"
    val app = AppConfig(
      host                      = host,
      port                      = port,
      runAPI                    = true,
      runUsersProjections       = true,
      runDiscussionsProjections = true
    )
    val api = APIConfig(
      info = APIInfo(
        title          = "test",
        version        = "test",
        description    = "test",
        termsOfService = "test",
        contact        = APIContact(name = "test", email = "test@brachtalk.io", url = s"http://$host:$port"),
        license        = APILicense(name = "test", url = s"http://$host:$port")
      ),
      pagination = Map.empty
    )
    app -> api
  }
}
