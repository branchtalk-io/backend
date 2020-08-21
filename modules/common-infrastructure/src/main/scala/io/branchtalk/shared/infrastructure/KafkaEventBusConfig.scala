package io.branchtalk.shared.infrastructure

import cats.data.NonEmptyList
import cats.effect.{ Concurrent, Sync, Timer }
import fs2.kafka._
import fs2.Pipe
import io.branchtalk.shared.models.UUID
import io.scalaland.catnip.Semi
import pureconfig._
import pureconfig.module.cats._

import scala.concurrent.duration._

@Semi(ConfigReader) final case class KafkaEventBusConfig(
  servers: NonEmptyList[Server],
  topic:   Topic
) {

  // TODO: add groupId to consumer!!!

  def toConsumerConfig[F[_]: Sync, Event: SafeDeserializer[F, *]]: ConsumerSettings[
    F,
    UUID,
    DeserializationError Either Event
  ] =
    ConsumerSettings(Deserializer.uuid[F], SafeDeserializer[F, Event])
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(servers.map(_.show).intercalate(","))
      .withGroupId("first-group")

  def toProducerConfig[F[_]: Sync, Event: Serializer[F, *]]: ProducerSettings[F, UUID, Event] =
    ProducerSettings(Serializer.uuid[F], Serializer[F, Event])
      .withBootstrapServers(servers.map(_.show).intercalate(","))

  // TODO: user config
  def toCommitBatch[F[_]: Concurrent: Timer]: Pipe[F, CommittableOffset[F], Unit] = commitBatchWithin[F](100, 5.seconds)
}
