package io.branchtalk.shared.infrastructure

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import doobie.util.transactor.Transactor
import io.branchtalk.shared.infrastructure.KafkaSerialization._
import io.prometheus.client.CollectorRegistry

// Utilities for connecting to database and events buses through Resources

final case class ReadsInfrastructure[F[_], Event](
  transactor: Transactor[F],
  consumer:   KafkaEventConsumerConfig => ConsumerStream[F, Event]
)

final case class WritesInfrastructure[F[_], Event, InternalEvent](
  transactor:             Transactor[F],
  internalProducer:       EventBusProducer[F, InternalEvent],
  internalConsumerStream: ConsumerStream[F, InternalEvent],
  producer:               EventBusProducer[F, Event],
  cache:                  Cache[F, String, Event]
)
final class DomainModule[Event: Encoder: Decoder: SchemaFor, InternalEvent: Encoder: Decoder: SchemaFor] {

  def setupReads[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig,
    registry:     CollectorRegistry
  ): Resource[F, ReadsInfrastructure[F, Event]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.databaseReads).transactor(registry)
      consumerStreamBuilder = ConsumerStream.fromConfigs[F, Event](domainConfig.publishedEventBus)
    } yield ReadsInfrastructure(transactor, consumerStreamBuilder)

  def setupWrites[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig,
    registry:     CollectorRegistry
  ): Resource[F, WritesInfrastructure[F, Event, InternalEvent]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.databaseWrites).transactor(registry)
      internalProducer       = KafkaEventBus.producer[F, InternalEvent](domainConfig.internalEventBus)
      internalConsumerStream = ConsumerStream.fromConfigs[F, InternalEvent](domainConfig.internalEventBus)
      producer               = KafkaEventBus.producer[F, Event](domainConfig.publishedEventBus)
      cache <- Cache.fromConfigs[F, Event](domainConfig.internalEventBus)
    } yield WritesInfrastructure(
      transactor,
      internalProducer,
      internalConsumerStream(domainConfig.internalConsumer),
      producer,
      cache
    )
}
object DomainModule {

  def apply[Event: Encoder: Decoder: SchemaFor, InternalEvent: Encoder: Decoder: SchemaFor]: DomainModule[
    Event,
    InternalEvent
  ] = new DomainModule[Event, InternalEvent]
}
