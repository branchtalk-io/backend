package io.branchtalk.shared.infrastructure

import io.scalaland.catnip.Semi
import pureconfig._

@Semi(ConfigReader) final case class DomainConfig(
  name:              DomainName,
  database:          PostgresConfig,
  publishedEventBus: KafkaEventBusConfig,
  internalEventBus:  KafkaEventBusConfig,
  consumers:         Map[String, KafkaEventConsumerConfig]
) {

  // assumes that each config has to have this field
  def internalConsumer: KafkaEventConsumerConfig = consumers("internal")
}
