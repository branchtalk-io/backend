package io.branchtalk.configs

import cats.effect.Sync
import hearth.kindlings.sconfigderivation.ConfigReader
import org.ekrich.config.{ Config, ConfigFactory }

import java.io.File
import scala.reflect.ClassTag

object Configuration {

  def getEnv[F[_]: Sync]: F[Map[String, String]] = Sync[F].delay(sys.env)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def readConfig[F[_]: Sync, A: ConfigReader: ClassTag](at: String): F[A] =
    Sync[F].delay {
      val resolved = finalConfig.resolve()
      summon[ConfigReader[A]].from(resolved.getValue(at)).fold(error => throw error, identity)
    }

  private def defaultConfigs: List[Config] =
    List(ConfigFactory.defaultOverrides(), ConfigFactory.defaultApplication(), ConfigFactory.defaultReference())

  private def configOverrides: List[Config] =
    (for {
      overridesFilesStrings <- sys.props.get("config.overrides")
      overrideFilesList = overridesFilesStrings
        .split(',')
        .view
        .filter(name => name.endsWith(".conf") || name.endsWith(".json"))
        .map(new File(_))
        .filter(_.exists())
        .toList
      if overrideFilesList.nonEmpty
    } yield overrideFilesList.map(ConfigFactory.parseFile)).getOrElse(List.empty)

  private def finalConfig: Config =
    (configOverrides ::: defaultConfigs)
      .reduceLeftOption(_.withFallback(_))
      .getOrElse(ConfigFactory.empty("branchtalk"))
}
