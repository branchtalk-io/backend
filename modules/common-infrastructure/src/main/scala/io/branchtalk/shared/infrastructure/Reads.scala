package io.branchtalk.shared.infrastructure

import cats.effect.Concurrent
import org.typelevel.doobie.*
import org.typelevel.doobie.implicits.*
import fs2.Stream

// Utilities for reads services.
abstract class Reads[F[_]: Concurrent, Entity](transactor: Transactor[F]) {

  // reading from DB Postgres as part of the read model

  final protected def queryOption(query: Query0[Entity]): F[Option[Entity]] = query.option.transact(transactor)

  final protected def queryList(query: Query0[Entity]): F[List[Entity]] = query.accumulate[List].transact(transactor)

  final protected def queryStream(query: Query0[Entity]): Stream[F, Entity] = query.stream.transact(transactor)
}
object Reads {

  final case class Infrastructure[F[_], Event](
    transactor: Transactor[F],
    consumer:   KafkaEventBus.ConsumerConfig => ConsumerStream[F, Event]
  )
}
