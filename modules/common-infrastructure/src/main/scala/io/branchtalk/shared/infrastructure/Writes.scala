package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.{ CodePosition, CommonError, ID, UUID }
import fs2.*

// Utilities for writes services.
abstract class Writes[F[_]: Sync, Entity, Event](producer: KafkaEventBus.Producer[F, Event]) {

  // sending event to internal bus as a part of a write model

  final protected def postEvent(id: ID[Entity], event: Event): F[Unit] =
    producer(Stream[F, (UUID, Event)](id.unwrap -> event)).compile.drain

  protected class EntityCheck(entity: String, transactor: Transactor[F]) {
    def apply(entityID: ID[Entity], fragment: Fragment)(using CodePosition): F[Unit] =
      fragment.exists(show"Check that $entity ID=$entityID exists").transact(transactor).flatMap {
        case true  => Sync[F].unit
        case false => (CommonError.notFound(entity, entityID): Throwable).raiseError[F, Unit]
      }
  }

  protected class ParentCheck[Parent](entity: String, transactor: Transactor[F]) {
    def apply(parentID: ID[Parent], fragment: Fragment)(using CodePosition): F[Unit] =
      fragment.exists(show"Check that parental $entity ID=$parentID exists").transact(transactor).flatMap {
        case true  => Sync[F].unit
        case false => (CommonError.parentNotExist(entity, parentID): Throwable).raiseError[F, Unit]
      }

    def withValue[T: Meta](parentID: ID[Parent], fragment: Fragment)(using CodePosition): F[T] =
      fragment
        .queryWithLabel[T](show"Check that parental $entity ID=$parentID exists")
        .option
        .transact(transactor)
        .flatMap {
          case Some(t) => Sync[F].pure(t)
          case None    => (CommonError.parentNotExist(entity, parentID): Throwable).raiseError[F, T]
        }
  }
}
object Writes {

  final case class Infrastructure[F[_], Event, InternalEvent](
    transactor:             Transactor[F],
    internalProducer:       KafkaEventBus.Producer[F, InternalEvent],
    internalConsumerStream: ConsumerStream[F, InternalEvent],
    producer:               KafkaEventBus.Producer[F, Event],
    consumerStream:         ConsumerStream.Factory[F, Event],
    cache:                  Cache[F, String, Event]
  )
}
