package io.branchtalk.shared.infrastructure

import cats.effect.Async
import com.typesafe.scalalogging.Logger
import fs2.Stream
import fs2.kafka._
import io.branchtalk.shared.model.UUID

object KafkaEventBus {

  private val logger = Logger(getClass)

  def producer[F[_]: Async, Event: Serializer[F, *]](
    settings: KafkaEventBusConfig
  ): EventBusProducer[F, Event] = (events: Stream[F, (UUID, Event)]) => {
    events
      .map { case (key, value) =>
        ProducerRecords.one(ProducerRecord(settings.topic.nonEmptyString.value, key, value))
      }
      .through(KafkaProducer.pipe(settings.toProducerConfig[F, Event]))
      .evalTap(e =>
        Async[F].delay(logger.info(show"${e.records.size} events published to ${settings.topic.nonEmptyString.value}"))
      )
  }

  def consumer[F[_]: Async, Event: SafeDeserializer[F, *]](
    busConfig:      KafkaEventBusConfig,
    consumerConfig: KafkaEventConsumerConfig
  ): EventBusConsumer[F, Event] =
    KafkaConsumer
      .stream(busConfig.toConsumerConfig[F, Event](consumerConfig))
      .evalTap(_.subscribeTo(busConfig.topic.nonEmptyString.value))
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
}
