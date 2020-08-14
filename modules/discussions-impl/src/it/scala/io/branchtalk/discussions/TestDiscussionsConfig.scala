package io.branchtalk.discussions

import cats.effect.{ Async, ContextShift, Resource, Sync }
import eu.timepit.refined.auto._
import io.branchtalk.shared.infrastructure.{
  DomainConfig,
  DomainName,
  TestKafkaEventBusConfig,
  TestPostgresConfig,
  TestResources
}
import io.scalaland.catnip.Semi
import pureconfig._

@Semi(ConfigReader) final case class TestDiscussionsConfig(
  database:          TestPostgresConfig,
  publishedEventBus: TestKafkaEventBusConfig,
  internalEventBus:  TestKafkaEventBusConfig
)
object TestDiscussionsConfig {

  def load[F[_]: Sync]: Resource[F, TestDiscussionsConfig] =
    Resource.liftF(
      Sync[F].delay(
        ConfigSource.resources("discussions-test.conf").at("discussions-test").loadOrThrow[TestDiscussionsConfig]
      )
    )

  def loadDomainConfig[F[_]: Async: ContextShift] =
    for {
      TestDiscussionsConfig(dbTestCfg, publishedESTestCfg, internalESTestCfg) <- TestDiscussionsConfig.load[F]
      dbCfg <- TestResources.postgresConfigResource[F](dbTestCfg)
      publishedESCfg <- TestResources.kafkaEventBusConfigResource[F](publishedESTestCfg)
      internalESCfg <- TestResources.kafkaEventBusConfigResource[F](internalESTestCfg)
    } yield DomainConfig(DomainName("discussions-test"), dbCfg, publishedESCfg, internalESCfg)
}
