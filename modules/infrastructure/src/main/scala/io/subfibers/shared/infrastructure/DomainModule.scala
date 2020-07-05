package io.subfibers.shared.infrastructure

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import doobie.util.transactor.Transactor
import fs2.kafka.{ Deserializer, Serializer }
import io.subfibers.shared.models.UUID

final case class Infrastructure[F[_], Event, InternalEvent](
  transactor:        Transactor[F],
  internalPublisher: EventBusPublisher[F, UUID, InternalEvent],
  internalConsumer:  EventBusSubscriber[F, UUID, InternalEvent],
  publisher:         EventBusPublisher[F, UUID, Event],
  consumer:          EventBusSubscriber[F, UUID, Event]
)
abstract class DomainModule[Event, InternalEvent] {

  // TODO: implement somehow
  implicit def keySerializer[F[_]]:             Serializer[F, UUID]            = ???
  implicit def keyDeserializer[F[_]]:           Deserializer[F, UUID]          = ???
  implicit def internalEventSerializer[F[_]]:   Serializer[F, InternalEvent]   = ???
  implicit def internalEventDeserializer[F[_]]: Deserializer[F, InternalEvent] = ???
  implicit def eventSerializer[F[_]]:           Serializer[F, Event]           = ???
  implicit def eventDeserializer[F[_]]:         Deserializer[F, Event]         = ???

  protected def setupInfrastructure[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, Infrastructure[F, Event, InternalEvent]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.database).transactor
      internalPublisher = KafkaEventBus.producer[F, UUID, InternalEvent](domainConfig.internalEventBus)
      internalConsumer  = KafkaEventBus.consumer[F, UUID, InternalEvent](domainConfig.internalEventBus)
      publisher         = KafkaEventBus.producer[F, UUID, Event](domainConfig.publishedEventBus)
      consumer          = KafkaEventBus.consumer[F, UUID, Event](domainConfig.publishedEventBus)
    } yield Infrastructure(transactor, internalPublisher, internalConsumer, publisher, consumer)
}
