package io.branchtalk.shared.infrastructure

import cats.effect.Async
import fs2.{ io => _, * }
import fs2.kafka.{ ProducerResult, Serializer }
import io.branchtalk.logging.Logger
import io.branchtalk.shared.model.UUID

// TODO: move to KafkaEventBus
final class ConsumerStream[F[_], Event](
  consumer:  KafkaEventBus.Consumer[F, Event],
  committer: KafkaEventBus.Committer[F]
) {

  // Runs pipe (projections) on events and commit them once they are processed.
  def runThrough[B](
    logger: Logger[F]
  )(f: Pipe[F, (String, Event), B])(using Async[F]): StreamRunner[F] = StreamRunner[F, Unit] {
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
  )(f: Pipe[F, (String, Event), B])(using Async[F]): StreamRunner[F] =
    runThrough(logger)(cache.piped(_._1, f))
}
object ConsumerStream {

  type Factory[F[_], Event] = KafkaEventBus.ConsumerConfig => ConsumerStream[F, Event]

  def fromConfigs[F[_]: Async, Event: Serializer[F, *]: SafeDeserializer[F, *]](
    busConfig: KafkaEventBus.BusConfig
  ): Factory[F, Event] =
    consumerCfg =>
      new ConsumerStream(
        consumer = KafkaEventBus.consumer[F, Event](busConfig, consumerCfg),
        committer = busConfig.toCommitBatch[F](consumerCfg)
      )

  def noID[F[_], A, B]: Pipe[F, (A, B), B] = _.map(_._2)

  def produced[F[_], A]: Pipe[F, ProducerResult[UUID, A], A] = _.flatMap(pr => Stream(pr.map(_._1.value).toList: _*))
}
