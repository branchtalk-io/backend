package io.branchtalk.shared.infrastructure

import cats.effect.{ Resource, Sync }
import org.apache.kafka.clients.admin.{ AdminClient, AdminClientConfig }

import scala.jdk.CollectionConverters._

trait TestKafkaResources extends TestResourcesHelpers {

  def kafkaEventBusConfigResource[F[_]: Sync](
    testKafkaEventBusConfig: TestKafkaEventBusConfig
  ): Resource[F, KafkaEventBusConfig] =
    Resource
      .eval(generateRandomSuffix[F])
      .flatMap(randomSuffix =>
        Resource.pure[F, KafkaEventBusConfig](testKafkaEventBusConfig.toKafkaEventBusConfig(randomSuffix))
      )
      .flatTap { cfg =>
        Resource.make {
          Sync[F].delay {
            AdminClient.create(
              Map[String, Object](
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG -> cfg.servers.mkString_(",")
              ).asJava
            )
          }
        } { client =>
          Sync[F].delay {
            try if (client.listTopics().names().get().asScala.contains(cfg.topic.nonEmptyString.value)) {
              client.deleteTopics(List(cfg.topic.nonEmptyString.value).asJavaCollection)
              ()
            } finally client.close()
          }
        }
      }
}
