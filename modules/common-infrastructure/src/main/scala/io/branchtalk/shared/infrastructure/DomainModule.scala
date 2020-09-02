package io.branchtalk.shared.infrastructure

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import doobie.util.transactor.Transactor
import io.branchtalk.shared.infrastructure.KafkaSerialization._

final case class ReadsInfrastructure[F[_], Event](
  transactor: Transactor[F],
  consumer:   KafkaEventConsumerConfig => ConsumerStream[F, Event]
)

final case class WritesInfrastructure[F[_], Event, InternalEvent](
  transactor:             Transactor[F],
  internalPublisher:      EventBusProducer[F, InternalEvent],
  internalConsumerStream: ConsumerStream[F, InternalEvent],
  publisher:              EventBusProducer[F, Event]
)

abstract class DomainModule[Event: Encoder: Decoder: SchemaFor, InternalEvent: Encoder: Decoder: SchemaFor] {

  protected def setupReads[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, ReadsInfrastructure[F, Event]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.database).transactor
    } yield ReadsInfrastructure(
      transactor,
      (cfg: KafkaEventConsumerConfig) =>
        ConsumerStream(
          consumer  = KafkaEventBus.consumer[F, Event](domainConfig.publishedEventBus, cfg),
          committer = domainConfig.internalEventBus.toCommitBatch[F](cfg)
        )
    )

  protected def setupWrites[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, WritesInfrastructure[F, Event, InternalEvent]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.database).transactor
      internalPublisher = KafkaEventBus.producer[F, InternalEvent](domainConfig.internalEventBus)
      internalConsumerStream = ConsumerStream(
        consumer =
          KafkaEventBus.consumer[F, InternalEvent](domainConfig.internalEventBus, domainConfig.internalConsumer),
        committer = domainConfig.internalEventBus.toCommitBatch[F](domainConfig.internalConsumer)
      )
      publisher = KafkaEventBus.producer[F, Event](domainConfig.publishedEventBus)
    } yield WritesInfrastructure(transactor, internalPublisher, internalConsumerStream, publisher)
}
