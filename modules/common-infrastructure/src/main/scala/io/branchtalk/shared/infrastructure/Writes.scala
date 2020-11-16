package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models.{ CodePosition, CommonError, ID, UUID }
import fs2._

abstract class Writes[F[_]: Sync, Entity, Event](producer: EventBusProducer[F, Event]) {

  // sending event to internal bus as a part of a write model

  final protected def postEvent(id: ID[Entity], event: Event): F[Unit] =
    producer(Stream[F, (UUID, Event)](id.uuid -> event)).compile.drain

  protected class EntityCheck(entity: String, transactor: Transactor[F]) {
    def apply(entityID: ID[Entity], fragment: Fragment)(implicit codePosition: CodePosition): F[Unit] =
      fragment.exists.transact(transactor).flatMap {
        case true  => Sync[F].unit
        case false => (CommonError.NotFound(entity, entityID, codePosition): Throwable).raiseError[F, Unit]
      }
  }

  protected class ParentCheck[Parent](entity: String, transactor: Transactor[F]) {
    def apply(parentID: ID[Parent], fragment: Fragment)(implicit codePosition: CodePosition): F[Unit] =
      fragment.exists.transact(transactor).flatMap {
        case true  => Sync[F].unit
        case false => (CommonError.ParentNotExist(entity, parentID, codePosition): Throwable).raiseError[F, Unit]
      }

    def withValue[T: Meta](parentID: ID[Parent], fragment: Fragment)(implicit codePosition: CodePosition): F[T] =
      fragment.query[T].option.transact(transactor).flatMap {
        case Some(t) => Sync[F].pure(t)
        case None    => (CommonError.ParentNotExist(entity, parentID, codePosition): Throwable).raiseError[F, T]
      }
  }
}
