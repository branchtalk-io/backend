package io.branchtalk.shared.infrastructure

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Sync, Timer }
import fs2.{ io => _, _ }
import fs2.kafka.{ ProducerResult, Serializer }
import io.branchtalk.shared.model.{ Logger, UUID }

final class ConsumerStream[F[_], Event](
  consumer:  EventBusConsumer[F, Event],
  committer: EventBusCommitter[F]
) {

  // Runs pipe (projections) on events and commit them once they are processed.
  // Projections start when you run F[Unit] and stop when you exit Resource.
  def withPipeToResource[B](
    logger: Logger[F]
  )(f:      Pipe[F, (String, Event), B])(implicit F: Sync[F]): Resource[F, F[Unit]] =
    KillSwitch.asStream[F, F[Unit]] { stream =>
      consumer
        .zip(stream)
        .flatMap { case (event, _) =>
          Stream(s"${event.record.topic}:${event.record.offset.toString}" -> event.record.value)
            .evalTap(_ => logger.info(s"Processing event key = ${event.record.key.toString}"))
            .through(f)
            .map(_ => event.offset)
        }
        .through(committer)
        .compile
        .drain
    }

  // Same as above but with cached results of each operation.
  def withCachedPipeToResource[B](
    logger: Logger[F],
    cache:  Cache[F, String, B]
  )(f:      Pipe[F, (String, Event), B])(implicit F: Sync[F]): Resource[F, F[Unit]] =
    withPipeToResource(logger)(cache.piped(_._1, f))
}
object ConsumerStream {

  def fromConfigs[F[_]: ConcurrentEffect: ContextShift: Timer, Event: Serializer[F, *]: SafeDeserializer[F, *]](
    busConfig: KafkaEventBusConfig
  ): KafkaEventConsumerConfig => ConsumerStream[F, Event] =
    consumerCfg =>
      new ConsumerStream(
        consumer = KafkaEventBus.consumer[F, Event](busConfig, consumerCfg),
        committer = busConfig.toCommitBatch[F](consumerCfg)
      )

  def noID[F[_], A, B]: Pipe[F, (A, B), B] = _.map(_._2)

  def produced[F[_], A]: Pipe[F, ProducerResult[UUID, A, Unit], A] =
    _.flatMap(pr => Stream(pr.records.map(_._1.value).toList: _*))
}
