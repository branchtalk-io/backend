package io.branchtalk.shared.infrastructure

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import cats.Applicative
import com.sksamuel.avro4s.{ Decoder, Encoder }
import doobie.util.transactor.Transactor
import io.branchtalk.shared.infrastructure.KafkaSerialization._
import io.branchtalk.shared.models.UUID

final case class ReadsInfrastructure[F[_], Event](
  transactor: Transactor[F],
  consumer:   EventBusConsumer[F, UUID, Event]
)

final case class WritesInfrastructure[F[_], Event, InternalEvent](
  transactor:        Transactor[F],
  internalPublisher: EventBusProducer[F, UUID, InternalEvent],
  internalConsumer:  EventBusConsumer[F, UUID, InternalEvent],
  publisher:         EventBusProducer[F, UUID, Event]
)

abstract class DomainModule[Event: Encoder: Decoder, InternalEvent: Encoder: Decoder] {

  protected def setupReads[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, ReadsInfrastructure[F, Event]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.database).transactor
      consumer = KafkaEventBus.consumer[F, UUID, Event](domainConfig.publishedEventBus)
    } yield ReadsInfrastructure(transactor, consumer)

  protected def setupWrites[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, WritesInfrastructure[F, Event, InternalEvent]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.database).transactor
      internalPublisher = KafkaEventBus.producer[F, UUID, InternalEvent](domainConfig.internalEventBus)
      internalConsumer  = KafkaEventBus.consumer[F, UUID, InternalEvent](domainConfig.internalEventBus)
      publisher         = KafkaEventBus.producer[F, UUID, Event](domainConfig.publishedEventBus)
    } yield WritesInfrastructure(transactor, internalPublisher, internalConsumer, publisher)

  protected def projectorAsResource[F[_]: Applicative](projector: F[(F[Unit], F[Unit])]): Resource[F, F[Unit]] =
    Resource.make(projector)(_._2).map(_._1)
}
