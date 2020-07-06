package io.subfibers.shared.infrastructure

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.Stream
import fs2.kafka._

object KafkaEventBus {

  def producer[F[_]: ConcurrentEffect: ContextShift, K: Serializer[F, *], V: Serializer[F, *]](
    settings: KafkaEventBusConfig
  ): EventBusProducer[F, K, V] =
    ((_: Stream[F, (K, V)]).map {
      case (key, value) =>
        ProducerRecords.one(ProducerRecord(settings.topic.value.value, key, value))
    }) andThen produce(settings.toProducerConfig[F, K, V])

  def consumer[F[_]: ConcurrentEffect: ContextShift: Timer, K: Deserializer[F, *], V: Deserializer[F, *]](
    settings: KafkaEventBusConfig
  ): EventBusConsumer[F, K, V] =
    consumerStream(settings.toConsumerConfig[F, K, V])
      .evalTap(_.subscribeTo(settings.topic.value.value))
      .flatMap(_.stream)
}
