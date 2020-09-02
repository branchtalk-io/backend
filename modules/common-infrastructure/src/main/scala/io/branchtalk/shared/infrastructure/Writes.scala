package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import io.branchtalk.shared.models.{ ID, UUID }
import fs2._

abstract class Writes[F[_]: Sync, Entity, Event](producer: EventBusProducer[F, Event]) {

  // sending event to internal bus as a part of a write model

  protected final def postEvent(id: ID[Entity], event: Event): F[Unit] =
    producer(Stream[F, (UUID, Event)](id.value -> event)).compile.drain
}
