package io.branchtalk.shared.infrastructure

import cats.data.NonEmptyList
import io.branchtalk.shared.infrastructure.PureconfigSupport.*
import io.scalaland.chimney.dsl.*
import neotype.*

final case class TestKafkaEventBusConfig(
  servers:     NonEmptyList[Server],
  topicPrefix: KafkaEventBus.Topic,
  cache:       Server
) derives ConfigReader {

  def topic(generatedSuffix: String): KafkaEventBus.Topic =
    KafkaEventBus.Topic.unsafeMake(topicPrefix.unwrap + generatedSuffix)

  def toKafkaEventBusConfig(generatedSuffix: String): KafkaEventBus.BusConfig =
    this.into[KafkaEventBus.BusConfig].withFieldConst(_.topic, topic(generatedSuffix)).transform
}
