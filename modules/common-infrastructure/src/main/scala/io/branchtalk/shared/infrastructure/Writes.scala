package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models.{ CodePosition, CommonError, ID, UUID }
import fs2._

abstract class Writes[F[_]: Sync, Entity, Event](producer: EventBusProducer[F, Event]) {

  // sending event to internal bus as a part of a write model

  protected final def postEvent(id: ID[Entity], event: Event): F[Unit] =
    producer(Stream[F, (UUID, Event)](id.value -> event)).compile.drain

  protected class ParentCheck[Parent](entity: String, transactor: Transactor[F]) {
    def apply(parentID: ID[Parent], fragment: Fragment)(implicit codePosition: CodePosition): F[Unit] =
      fragment.exists.transact(transactor).flatMap {
        case true  => Sync[F].unit
        case false => (CommonError.ParentNotExist(entity, parentID, codePosition): Throwable).raiseError[F, Unit]
      }
  }

  // TODO: deleted check, restored check
}
