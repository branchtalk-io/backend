package io.subfibers.shared.infrastructure

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.{ Pipe, Stream }
import fs2.kafka._

object KafkaEventBus {

  def producer[F[_]: ConcurrentEffect: ContextShift, K: Serializer[F, *], V: Serializer[F, *], P](
    settings: KafkaEventBusConfig
  ): Pipe[F, (K, V), ProducerResult[K, V, Unit]] = {
    val wrap: Pipe[F, (K, V), ProducerRecords[K, V, Unit]] = _.map {
      case (key, value) =>
        ProducerRecords.one(ProducerRecord(settings.topic.value.value, key, value))
    }

    wrap andThen produce(settings.toProducerConfig[F, K, V])
  }

  def consumer[F[_]: ConcurrentEffect: ContextShift: Timer, K: Deserializer[F, *], V: Deserializer[F, *]](
    settings: KafkaEventBusConfig
  ): Stream[F, CommittableConsumerRecord[F, K, V]] =
    consumerStream(settings.toConsumerConfig[F, K, V])
      .evalTap(_.subscribeTo(settings.topic.value.value))
      .flatMap(_.stream)
}
