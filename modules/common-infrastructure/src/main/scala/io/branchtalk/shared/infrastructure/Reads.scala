package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import doobie._
import doobie.implicits._
import fs2.Stream

abstract class Reads[F[_]: Sync, Entity](transactor: Transactor[F]) {

  // reading from DB Postgres as part of the read model

  protected final def queryOption(query: Query0[Entity]): F[Option[Entity]] = query.option.transact(transactor)

  protected final def queryList(query: Query0[Entity]): F[List[Entity]] = query.accumulate[List].transact(transactor)

  protected final def queryStream(query: Query0[Entity]): Stream[F, Entity] = query.stream.transact(transactor)
}
