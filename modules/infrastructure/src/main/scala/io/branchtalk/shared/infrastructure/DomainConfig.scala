package io.branchtalk.shared.infrastructure

import pureconfig._
import pureconfig.generic.semiauto._

final case class DomainConfig(
  name:              DomainName,
  database:          PostgresConfig,
  publishedEventBus: KafkaEventBusConfig,
  internalEventBus:  KafkaEventBusConfig
)
object DomainConfig {

  implicit val configReader: ConfigReader[DomainConfig] = deriveReader[DomainConfig]
}
