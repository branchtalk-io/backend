package io.branchtalk.shared.infrastructure

import cats.effect.Async
import fs2.{ io => _, _ }
import fs2.kafka.{ ProducerResult, Serializer }
import io.branchtalk.shared.model.{ Logger, UUID }

final class ConsumerStream[F[_], Event](
  consumer:  EventBusConsumer[F, Event],
  committer: EventBusCommitter[F]
) {

  // Runs pipe (projections) on events and commit them once they are processed.
  def runThrough[B](
    logger: Logger[F]
  )(f:      Pipe[F, (String, Event), B])(implicit F: Async[F]): StreamRunner[F] = StreamRunner[F, Unit] {
    consumer
      .flatMap { event =>
        Stream(s"${event.record.topic}:${event.record.offset.toString}" -> event.record.value)
          .evalTap(_ => logger.info(s"Processing event key = ${event.record.key.toString}"))
          .through(f)
          .map(_ => event.offset)
      }
      .through(committer)
  }

  // Same as above but with cached results of each operation.
  def runCachedThrough[B](
    logger: Logger[F],
    cache:  Cache[F, String, B]
  )(f:      Pipe[F, (String, Event), B])(implicit F: Async[F]): StreamRunner[F] =
    runThrough(logger)(cache.piped(_._1, f))
}
object ConsumerStream {

  type Factory[F[_], Event] = KafkaEventConsumerConfig => ConsumerStream[F, Event]

  def fromConfigs[F[_]: Async, Event: Serializer[F, *]: SafeDeserializer[F, *]](
    busConfig: KafkaEventBusConfig
  ): Factory[F, Event] =
    consumerCfg =>
      new ConsumerStream(
        consumer = KafkaEventBus.consumer[F, Event](busConfig, consumerCfg),
        committer = busConfig.toCommitBatch[F](consumerCfg)
      )

  def noID[F[_], A, B]: Pipe[F, (A, B), B] = _.map(_._2)

  def produced[F[_], A]: Pipe[F, ProducerResult[Unit, UUID, A], A] =
    _.flatMap(pr => Stream(pr.records.map(_._1.value).toList: _*))
}
