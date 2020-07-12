package io.branchtalk.shared.infrastructure

import io.scalaland.catnip.Semi
import pureconfig._

@Semi(ConfigReader) final case class DomainConfig(
  name:              DomainName,
  database:          PostgresConfig,
  publishedEventBus: KafkaEventBusConfig,
  internalEventBus:  KafkaEventBusConfig
)
