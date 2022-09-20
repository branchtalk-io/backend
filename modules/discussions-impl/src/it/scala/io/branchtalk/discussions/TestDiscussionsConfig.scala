package io.branchtalk.discussions

import cats.effect.{ Async, Resource, Sync }
import io.branchtalk.shared.infrastructure.{
  DomainConfig,
  DomainName,
  KafkaEventConsumerConfig,
  TestKafkaEventBusConfig,
  TestPostgresConfig,
  TestResources
}
import io.branchtalk.shared.infrastructure.PureconfigSupport._
import io.scalaland.catnip.Semi

@Semi(ConfigReader) final case class TestDiscussionsConfig(
  database:          TestPostgresConfig,
  publishedEventBus: TestKafkaEventBusConfig,
  internalEventBus:  TestKafkaEventBusConfig,
  consumers:         Map[String, KafkaEventConsumerConfig]
)
object TestDiscussionsConfig {

  def load[F[_]: Sync]: Resource[F, TestDiscussionsConfig] =
    Resource.eval(
      Sync[F].delay(
        ConfigSource.resources("discussions-test.conf").at("discussions-test").loadOrThrow[TestDiscussionsConfig]
      )
    )

  def loadDomainConfig[F[_]: Async]: Resource[F, DomainConfig] =
    for {
      TestDiscussionsConfig(dbTest, publishedESTest, internalESTest, consumers) <- TestDiscussionsConfig.load[F]
      db <- TestResources.postgresConfigResource[F](dbTest)
      publishedES <- TestResources.kafkaEventBusConfigResource[F](publishedESTest)
      internalES <- TestResources.kafkaEventBusConfigResource[F](internalESTest)
    } yield DomainConfig(DomainName("discussions-test"), db, db, publishedES, internalES, consumers)
}
