package io.subfibers.shared.infrastructure

final case class DomainConfig(
  name:              DomainName,
  database:          PostgresConfig,
  publishedEventBus: KafkaEventBusConfig,
  internalEventBus:  KafkaEventBusConfig
)
