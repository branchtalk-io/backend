package io.subfibers.shared.infrastructure

import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import io.subfibers.shared.models.{ ID, UUID }
import fs2._

abstract class Writes[F[_]: Sync, Entity, Event](
  transactor: Transactor[F],
  publisher:  EventBusProducer[F, UUID, Event]
) {

  // sending event to internal bus as a part of a write model

  protected final def postEvent(id: ID[Entity], event: Event): F[Unit] =
    publisher(Stream[F, (UUID, Event)](id.value -> event)).compile.drain

  // TODO: move it to Reads
  // reading from database as a part of a read model

  protected final def queryOption(query: Query0[Entity]): F[Option[Entity]] = query.option.transact(transactor)

  protected final def queryList(query: Query0[Entity]): F[List[Entity]] = query.accumulate[List].transact(transactor)

  protected final def queryStream(query: Query0[Entity]): Stream[F, Entity] = query.stream.transact(transactor)
}
