package io.branchtalk.shared.infrastructure

import cats.Monad
import dev.profunktor.redis4cats.RedisCommands

trait Cache[F[_], K, V] {

  def apply(key: K)(value: F[V]): F[(K, V)]
}
object Cache {

  def fromRedis[F[_]: Monad, K, V](redis: RedisCommands[F, K, V]): Cache[F, K, V] = new Cache[F, K, V] {

    override def apply(key: K)(valueF: F[V]): F[(K, V)] = redis.get(key).flatMap {
      case Some(value) => (key -> value).pure[F]
      case None        => valueF.flatTap(redis.set(key, _)).map(key -> _)
    }
  }
}
