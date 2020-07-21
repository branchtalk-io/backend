package io.branchtalk.shared.infrastructure

import cats.data.NonEmptyList
import cats.effect.Sync
import fs2.kafka._
import io.scalaland.catnip.Semi
import pureconfig._
import pureconfig.module.cats._

@Semi(ConfigReader) final case class KafkaEventBusConfig(
  servers: NonEmptyList[Server],
  topic:   Topic
) {

  // TODO: add groupId to consumer!!!

  def toConsumerConfig[F[_]: Sync, K: Deserializer[F, *], V: Deserializer[F, *]]: ConsumerSettings[F, K, V] =
    ConsumerSettings(Deserializer[F, K], Deserializer[F, V]).withBootstrapServers(servers.map(_.show).intercalate(","))

  def toProducerConfig[F[_]: Sync, K: Serializer[F, *], V: Serializer[F, *]]: ProducerSettings[F, K, V] =
    ProducerSettings(Serializer[F, K], Serializer[F, V]).withBootstrapServers(servers.map(_.show).intercalate(","))
}
