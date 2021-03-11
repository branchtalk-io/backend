package io.branchtalk.shared.infrastructure

import java.nio.ByteBuffer
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Sync }
import cats.effect.syntax.all._
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import fs2.kafka.{ Headers, Serializer }
import fs2.{ Pipe, Stream }
import io.branchtalk.shared.model.{ Logger, branchtalkCharset }
import io.lettuce.core.codec.{ RedisCodec => JRedisCodec }

import scala.annotation.nowarn
import scala.util.control.NoStackTrace

// Wrapper around Redis Cache to cache operations in certain fs2.Streams for idempotency.
abstract class Cache[F[_]: Sync, K, V] {

  def apply(key: K)(value: F[V]): F[(K, V)]

  final private case object EmptyStream extends Exception with NoStackTrace

  @SuppressWarnings(Array("org.wartremover.warts.Throw")) // will be handled just after use
  private def unliftPipe[I](pipe: Pipe[F, I, V]): I => F[V] =
    i => Stream(i).through(pipe).compile.last.map(_.getOrElse(throw EmptyStream))

  def piped[I](key: I => K, pipe: Pipe[F, I, V]): Pipe[F, I, V] =
    (_: Stream[F, I]).flatMap(i => Stream.eval(apply(key(i))(unliftPipe(pipe)(i)))).map(_._2).handleErrorWith {
      case EmptyStream => Stream.empty
      case err         => Stream.raiseError[F](err)
    }
}
object Cache {

  def fromRedis[F[_]: Sync, K, V](redis: RedisCommands[F, K, V]): Cache[F, K, V] = new Cache[F, K, V] {

    override def apply(key: K)(valueF: F[V]): F[(K, V)] = redis.get(key).flatMap {
      case Some(value) => (key -> value).pure[F]
      case None        => valueF.flatTap(redis.set(key, _)).map(key -> _)
    }
  }

  def fromConfigs[F[_]: ConcurrentEffect: ContextShift, Event: Serializer[F, *]: SafeDeserializer[F, *]](
    busConfig: KafkaEventBusConfig
  ): Resource[F, Cache[F, String, Event]] =
    for {
      logger <- Resource.liftF(Logger.fromClass[F](classOf[Cache[F, String, Event]]))
      implicit0(log: Log[F]) = new Log[F] {
        override def debug(msg: => String): F[Unit] = logger.debug(msg)
        override def error(msg: => String): F[Unit] = logger.error(msg)
        override def info(msg:  => String): F[Unit] = logger.info(msg)
      }
      redis <- Redis[F].simple(
        show"redis://${busConfig.cache.host.value}:${busConfig.cache.port.value}",
        prepareCodec[F, Event](busConfig.topic.nonEmptyString.value)
      )
    } yield fromRedis(redis)

  private def prepareCodec[F[_]: ConcurrentEffect, Event: Serializer[F, *]: SafeDeserializer[F, *]](
    topic: String
  ): RedisCodec[String, Event] = RedisCodec(
    new JRedisCodec[String, Event] {
      override def decodeKey(bytes: ByteBuffer): String = new String(bytes.array(), branchtalkCharset)

      @SuppressWarnings(
        Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Null", "org.wartremover.warts.ToString")
      ) // null = empty cache
      override def decodeValue(bytes: ByteBuffer): Event =
        if (bytes.hasArray) {
          SafeDeserializer[F, Event]
            .deserialize(topic, Headers.empty, bytes.array())
            .flatMap {
              case Left(error)  => new Exception(error.toString).raiseError[F, Event]
              case Right(value) => value.pure[F]
            }
            .toIO
            .unsafeRunSync()
        } else null.asInstanceOf[Event] // scalastyle:ignore null

      override def encodeKey(key: String): ByteBuffer = ByteBuffer.wrap(key.getBytes(branchtalkCharset))

      override def encodeValue(value: Event): ByteBuffer =
        Serializer[F, Event].serialize(topic, Headers.empty, value).map(ByteBuffer.wrap).toIO.unsafeRunSync()
    }
  )
}
