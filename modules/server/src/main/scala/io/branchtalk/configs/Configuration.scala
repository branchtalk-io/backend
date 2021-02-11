package io.branchtalk.configs

import cats.data.NonEmptyList
import cats.effect.Sync
import io.branchtalk.shared.infrastructure.PureconfigSupport._

import java.io.File
import scala.reflect.ClassTag

object Configuration {

  def getEnv[F[_]: Sync]: F[Map[String, String]] = Sync[F].delay(sys.env)

  def readConfig[F[_]: Sync, A: ConfigReader: ClassTag](at: String): F[A] =
    Sync[F].delay(finalConfigs.reduceLeft(_.withFallback(_)).at(at).loadOrThrow[A])

  private def defaultConfigs =
    NonEmptyList.of(ConfigSource.defaultOverrides, ConfigSource.defaultApplication, ConfigSource.defaultReference)

  private def configOverrides =
    for {
      overridesFilesStrings <- sys.props.get("config.overrides")
      overrideFilesList = overridesFilesStrings
        .split(',')
        .view
        .filter(name => name.endsWith(".conf") || name.endsWith(".json"))
        .map(new File(_))
        .filter(_.exists())
        .toList
      overrideFilesNel <- NonEmptyList.fromList(overrideFilesList)
    } yield overrideFilesNel.map(ConfigSource.file)

  private def finalConfigs = configOverrides.fold(defaultConfigs)(_.concatNel(defaultConfigs))
}
