package io.branchtalk.shared.infrastructure

import cats.data.NonEmptyList
import cats.effect.{ Concurrent, Sync, Timer }
import fs2.kafka._
import fs2.Pipe
import io.branchtalk.shared.model.{ ShowPretty, UUID }
import io.branchtalk.shared.infrastructure.PureconfigSupport._
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import io.scalaland.catnip.Semi

@Semi(ConfigReader, ShowPretty) final case class KafkaEventBusConfig(
  servers: NonEmptyList[Server],
  topic:   Topic,
  cache:   Server
) {

  def toConsumerConfig[F[_]: Sync, Event: SafeDeserializer[F, *]](
    consumerConfig: KafkaEventConsumerConfig
  ): ConsumerSettings[F, UUID, DeserializationResult[Event]] =
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
