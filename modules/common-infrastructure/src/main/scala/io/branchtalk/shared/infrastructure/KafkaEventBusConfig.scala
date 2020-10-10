package io.branchtalk.shared.infrastructure

import cats.data.NonEmptyList
import cats.effect.{ Concurrent, Sync, Timer }
import fs2.kafka._
import fs2.Pipe
import io.branchtalk.shared.models.UUID
import io.scalaland.catnip.Semi
import pureconfig._
import pureconfig.module.cats._

@Semi(ConfigReader) final case class KafkaEventBusConfig(
  servers: NonEmptyList[Server],
  topic:   Topic
) {

  def toConsumerConfig[F[_]: Sync, Event: SafeDeserializer[F, *]](
    consumerConfig: KafkaEventConsumerConfig
  ): ConsumerSettings[F, UUID, DeserializationError Either Event] =
    ConsumerSettings(Deserializer.uuid[F], SafeDeserializer[F, Event])
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(servers.map(_.show).intercalate(","))
      .withGroupId(consumerConfig.consumerGroup.nonEmptyString.value)

  def toProducerConfig[F[_]: Sync, Event: Serializer[F, *]]: ProducerSettings[F, UUID, Event] =
    ProducerSettings(Serializer.uuid[F], Serializer[F, Event])
      .withBootstrapServers(servers.map(_.show).intercalate(","))

  def toCommitBatch[F[_]: Concurrent: Timer](
    consumerConfig: KafkaEventConsumerConfig
  ): Pipe[F, CommittableOffset[F], Unit] =
    commitBatchWithin[F](consumerConfig.maxCommitSize.positiveInt.value, consumerConfig.maxCommitTime.finiteDuration)
}
