package io.branchtalk.notifications

import cats.effect.{ Async, Resource, Sync }
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }
import org.ekrich.config.ConfigFactory

final case class TestNotificationsConfig(
  database:          TestPostgresConfig,
  publishedEventBus: TestKafkaEventBusConfig,
  internalEventBus:  TestKafkaEventBusConfig,
  consumers:         Map[String, KafkaEventBus.ConsumerConfig]
) derives ConfigReader
object TestNotificationsConfig {

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def load[F[_]: Sync]: Resource[F, TestNotificationsConfig] =
    Resource.eval(
      Sync[F].delay {
        val config = ConfigFactory.parseResources("notifications-test.conf").resolve()
        summon[ConfigReader[TestNotificationsConfig]]
          .from(config.getValue("notifications-test"))
          .fold(error => throw error, identity)
      }
    )

  def loadDomainConfig[F[_]: Async]: Resource[F, DomainModule.Config] =
    for {
      TestNotificationsConfig(dbTest, publishedESTest, internalESTest, consumers) <- TestNotificationsConfig.load[F]
      db <- TestResources.postgresConfigResource[F](dbTest)
      publishedES <- TestResources.kafkaEventBusConfigResource[F](publishedESTest)
      internalES <- TestResources.kafkaEventBusConfigResource[F](internalESTest)
    } yield DomainModule.Config(DomainModule.Name("notifications-test"), db, db, publishedES, internalES, consumers)
}
