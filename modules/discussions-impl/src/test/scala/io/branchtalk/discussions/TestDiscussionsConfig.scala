package io.branchtalk.discussions

import cats.effect.{ Async, Resource, Sync }
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }

final case class TestDiscussionsConfig(
  database:          TestPostgresConfig,
  publishedEventBus: TestKafkaEventBusConfig,
  internalEventBus:  TestKafkaEventBusConfig,
  consumers:         Map[String, KafkaEventBus.ConsumerConfig]
) derives ConfigReader
object TestDiscussionsConfig {

  def load[F[_]: Sync]: Resource[F, TestDiscussionsConfig] =
    Resource.eval(
      Sync[F].delay(
        ConfigSource.resources("discussions-test.conf").at("discussions-test").loadOrThrow[TestDiscussionsConfig]
      )
    )

  def loadDomainConfig[F[_]: Async]: Resource[F, DomainModule.Config] =
    for {
      TestDiscussionsConfig(dbTest, publishedESTest, internalESTest, consumers) <- TestDiscussionsConfig.load[F]
      db <- TestResources.postgresConfigResource[F](dbTest)
      publishedES <- TestResources.kafkaEventBusConfigResource[F](publishedESTest)
      internalES <- TestResources.kafkaEventBusConfigResource[F](internalESTest)
    } yield DomainModule.Config(DomainModule.Name.unsafeMake("discussions-test"),
                                db,
                                db,
                                publishedES,
                                internalES,
                                consumers
    )
}
