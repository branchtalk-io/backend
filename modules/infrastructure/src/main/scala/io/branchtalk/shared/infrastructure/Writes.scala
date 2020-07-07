package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import io.branchtalk.shared.models.{ ID, UUID }
import fs2._

abstract class Writes[F[_]: Sync, Entity, Event](publisher: EventBusProducer[F, UUID, Event]) {

  // sending event to internal bus as a part of a write model

  protected final def postEvent(id: ID[Entity], event: Event): F[Unit] =
    publisher(Stream[F, (UUID, Event)](id.value -> event)).compile.drain
}
