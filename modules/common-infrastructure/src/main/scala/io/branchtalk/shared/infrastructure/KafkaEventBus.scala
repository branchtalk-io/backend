package io.branchtalk.shared.infrastructure

import cats.Show
import cats.data.NonEmptyList
import cats.effect.{ Async, Concurrent, Sync, Temporal }
import com.typesafe.scalalogging.Logger
import fs2.{ Pipe, Stream }
import fs2.kafka.*
import io.branchtalk.shared.model.{ ShowPretty, UUID }
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult

import scala.concurrent.duration.FiniteDuration

object KafkaEventBus {

  type Producer[F[_], Event] = Pipe[F, (UUID, Event), ProducerResult[UUID, Event]]
  type Consumer[F[_], Event] = Stream[F, CommittableConsumerRecord[F, UUID, Event]]
  type Committer[F[_]]       = Pipe[F, CommittableOffset[F], Unit]

  private val logger = Logger(getClass)

  def producer[F[_]: Async, Event: Serializer[F, *]](busConfig: BusConfig): Producer[F, Event] =
    (events: Stream[F, (UUID, Event)]) =>
      events
        .map { case (key, value) =>
          ProducerRecords.one(ProducerRecord(busConfig.topic.unwrap, key, value))
        }
        .through(KafkaProducer.pipe(busConfig.toProducerConfig[F, Event]))
        .evalTap(e => Async[F].delay(logger.info(show"${e.size} events published to ${busConfig.topic}")))

  def consumer[F[_]: Async, Event: SafeDeserializer[F, *]](
    busConfig:      BusConfig,
    consumerConfig: ConsumerConfig
  ): Consumer[F, Event] =
    KafkaConsumer
      .stream(busConfig.toConsumerConfig[F, Event](consumerConfig))
      .evalTap(_.subscribeTo(busConfig.topic.unwrap))
      .flatMap(_.stream)
      .flatMap { commitable =>
        commitable.record.value match {
          case Right(value) =>
            Stream(copyRecord(commitable, value))
          case Left(err2) =>
            logger.error(s"Failed value deserialization: $err2")
            Stream.empty
        }
      }

  private def copyRecord[F[_], V1, V2](
    commitable: CommittableConsumerRecord[F, UUID, V1],
    value:      V2
  ): CommittableConsumerRecord[F, UUID, V2] = {
    val CommittableConsumerRecord(record, offset) = commitable
    CommittableConsumerRecord(ConsumerRecord(record.topic, record.partition, record.offset, record.key, value), offset)
  }

  type Topic = Topic.Type
  object Topic extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(topic: Topic): Some[String] = Some(topic.unwrap)

    given ConfigReader[Topic] = ConfigReader[String].emapString("Topic")(make)
    given Show[Topic]         = unsafeMakeF[Show](Show[String])
  }

  type ConsumerGroup = ConsumerGroup.Type
  object ConsumerGroup extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(consumerGroup: ConsumerGroup): Some[String] = Some(consumerGroup.unwrap)

    given ConfigReader[ConsumerGroup] = ConfigReader[String].emapString("ConsumerGroup")(make)
    given Show[ConsumerGroup]         = unsafeMakeF[Show](Show[String])
  }

  type MaxCommitSize = MaxCommitSize.Type
  object MaxCommitSize extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input > 0

    def unapply(domainName: MaxCommitSize): Some[Int] = Some(domainName.unwrap)

    given ConfigReader[MaxCommitSize] = ConfigReader[Int].emapString("MaxCommitSize")(make)
    given Show[MaxCommitSize]         = unsafeMakeF[Show](Show[Int])
  }

  type MaxCommitTime = MaxCommitTime.Type
  object MaxCommitTime extends Newtype[FiniteDuration] {

    def unapply(maxCommitTime: MaxCommitTime): Some[FiniteDuration] = Some(maxCommitTime.unwrap)

    given ConfigReader[MaxCommitTime] = unsafeMakeF[ConfigReader](ConfigReader[FiniteDuration])
    given Show[MaxCommitTime]         = unsafeMakeF[Show](Show[FiniteDuration])
  }

  final case class BusConfig(
    servers: NonEmptyList[Server],
    topic:   Topic,
    cache:   Server
  ) derives ConfigReader,
        ShowPretty {

    def toConsumerConfig[F[_]: Sync, Event: SafeDeserializer[F, *]](
      consumerConfig: ConsumerConfig
    ): ConsumerSettings[F, UUID, DeserializationResult[Event]] =
      ConsumerSettings(Deserializer.uuid[F], SafeDeserializer[F, Event])
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers(servers.map(_.show).intercalate(","))
        .withGroupId(consumerConfig.consumerGroup.unwrap)

    def toProducerConfig[F[_]: Sync, Event: Serializer[F, *]]: ProducerSettings[F, UUID, Event] =
      ProducerSettings(Serializer.uuid[F], Serializer[F, Event])
        .withBootstrapServers(servers.map(_.show).intercalate(","))

    def toCommitBatch[F[_]: Concurrent: Temporal](
      consumerConfig: ConsumerConfig
    ): Pipe[F, CommittableOffset[F], Unit] =
      commitBatchWithin[F](consumerConfig.maxCommitSize.unwrap, consumerConfig.maxCommitTime.unwrap)
  }

  final case class ConsumerConfig(
    consumerGroup: ConsumerGroup,
    maxCommitSize: MaxCommitSize,
    maxCommitTime: MaxCommitTime
  ) derives ConfigReader,
        ShowPretty
}
