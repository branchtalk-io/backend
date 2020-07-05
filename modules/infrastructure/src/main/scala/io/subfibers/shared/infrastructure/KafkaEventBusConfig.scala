package io.subfibers.shared.infrastructure

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import fs2.kafka._

final case class KafkaEventBusConfig(
  servers: NonEmptyList[Server],
  topic:   Topic
) {

  def toConsumerConfig[F[_]: Sync, K: Deserializer[F, *], V: Deserializer[F, *]]: ConsumerSettings[F, K, V] =
    ConsumerSettings(Deserializer[F, K], Deserializer[F, V]).withBootstrapServers(
      servers.map { case Server(host, port) => s"${host.value}:${port.value.toString}" }.intercalate(",")
    )

  def toProducerConfig[F[_]: Sync, K: Serializer[F, *], V: Serializer[F, *]]: ProducerSettings[F, K, V] =
    ProducerSettings(Serializer[F, K], Serializer[F, V]).withBootstrapServers(
      servers.map { case Server(host, port) => s"${host.value}:${port.value.toString}" }.intercalate(",")
    )
}
