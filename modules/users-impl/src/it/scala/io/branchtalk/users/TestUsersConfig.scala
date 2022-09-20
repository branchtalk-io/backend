package io.branchtalk.users

import cats.effect.{ Async, Resource, Sync }
import io.branchtalk.shared.infrastructure.{
  DomainConfig,
  DomainName,
  KafkaEventConsumerConfig,
  TestKafkaEventBusConfig,
  TestPostgresConfig,
  TestResources
}
import io.scalaland.catnip.Semi
import pureconfig.{ ConfigReader, ConfigSource }

@Semi(ConfigReader) final case class TestUsersConfig(
  database:          TestPostgresConfig,
  publishedEventBus: TestKafkaEventBusConfig,
  internalEventBus:  TestKafkaEventBusConfig,
  consumers:         Map[String, KafkaEventConsumerConfig]
)
object TestUsersConfig {

  def load[F[_]: Sync]: Resource[F, TestUsersConfig] =
    Resource.eval(
      Sync[F].delay(
        ConfigSource.resources("users-test.conf").at("users-test").loadOrThrow[TestUsersConfig]
      )
    )

  def loadDomainConfig[F[_]: Async]: Resource[F, DomainConfig] =
    for {
      TestUsersConfig(dbTest, publishedESTest, internalESTest, consumers) <- TestUsersConfig.load[F]
      db <- TestResources.postgresConfigResource[F](dbTest)
      publishedES <- TestResources.kafkaEventBusConfigResource[F](publishedESTest)
      internalES <- TestResources.kafkaEventBusConfigResource[F](internalESTest)
    } yield DomainConfig(DomainName("discussions-test"), db, db, publishedES, internalES, consumers)
}
