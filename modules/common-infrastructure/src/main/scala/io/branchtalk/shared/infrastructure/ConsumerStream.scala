package io.branchtalk.shared.infrastructure

import java.nio.ByteBuffer

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Sync, Timer }
import cats.effect.syntax.all._
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout._
import dev.profunktor.redis4cats.data.RedisCodec
import fs2.{ io => _, _ }
import fs2.kafka.{ Headers, Serializer }
import io.branchtalk.shared.model.{ Logger, UUID }
import io.branchtalk.shared.model.UUIDGenerator.FastUUIDGenerator
import io.lettuce.core.codec.{ RedisCodec => JRedisCodec }

final class ConsumerStream[F[_], Event](
  consumer:  EventBusConsumer[F, Event],
  committer: EventBusCommitter[F],
  cache:     Cache[F, UUID, Event]
) {

  // Runs pipe (projections) on events and commit them once they are processed.
  // Projections start when you run F[Unit] and stop when you exit Resource.
  def withPipeToResource[B](logger: Logger[F])(f: Pipe[F, Event, B])(implicit F: Sync[F]): Resource[F, F[Unit]] =
    KillSwitch.asStream[F, F[Unit]] { stream =>
      consumer
        .zip(stream)
        .flatMap { case (event, _) =>
          Stream(event.record.value)
            .evalTap(_ => logger.info(s"Processing event key = ${event.record.key.toString}"))
            .through(f)
            .map(_ => event.offset)
        }
        .through(committer)
        .compile
        .drain
    }
}
object ConsumerStream {

  private def prepareCodec[F[_]: ConcurrentEffect, Event: Serializer[F, *]: SafeDeserializer[F, *]](
    topic: String
  ): RedisCodec[UUID, Event] = RedisCodec(
    new JRedisCodec[UUID, Event] {
      override def decodeKey(bytes: ByteBuffer): UUID =
        FastUUIDGenerator.parse[F](new String(bytes.array())).toIO.unsafeRunSync()

      override def decodeValue(bytes: ByteBuffer): Event =
        SafeDeserializer[F, Event]
          .deserialize(topic, Headers.empty, bytes.array())
          .flatMap {
            case Left(error)  => new Exception(error.toString).raiseError[F, Event]
            case Right(value) => value.pure[F]
          }
          .toIO
          .unsafeRunSync()

      override def encodeKey(key: UUID): ByteBuffer =
        ByteBuffer.wrap(key.toString.getBytes)

      override def encodeValue(value: Event): ByteBuffer =
        Serializer[F, Event].serialize(topic, Headers.empty, value).map(ByteBuffer.wrap).toIO.unsafeRunSync()
    }
  )

  def fromConfigs[F[_]: ConcurrentEffect: ContextShift: Timer, Event: Serializer[F, *]: SafeDeserializer[F, *]](
    busConfig: KafkaEventBusConfig
  ): Resource[F, KafkaEventConsumerConfig => ConsumerStream[F, Event]] =
    Redis[F]
      .simple(
        s"redis://${busConfig.cache.host.value}:${busConfig.cache.port}",
        prepareCodec[F, Event](busConfig.topic.nonEmptyString.value)
      )
      .map { redis => consumerCfg =>
        new ConsumerStream(
          consumer = KafkaEventBus.consumer[F, Event](busConfig, consumerCfg),
          committer = busConfig.toCommitBatch[F](consumerCfg),
          cache = Cache.fromRedis(redis)
        )
      }
}
