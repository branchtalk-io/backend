package io.branchtalk.configs

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.PureconfigSupport._

import scala.reflect.ClassTag

object Configuration {

  def getEnv[F[_]: Sync]: F[Map[String, String]] = Sync[F].delay(sys.env)

  def readConfig[F[_]: Sync, A: ConfigReader: ClassTag](at: String): F[A] =
    Sync[F].delay(ConfigSource.defaultApplication.at(at).loadOrThrow[A])
}
