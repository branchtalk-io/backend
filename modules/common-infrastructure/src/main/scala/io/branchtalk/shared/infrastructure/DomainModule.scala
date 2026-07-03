package io.branchtalk.shared.infrastructure

import cats.Show
import cats.effect.std.Dispatcher
import cats.effect.{ Async, Resource }
import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import doobie.util.transactor.Transactor
import io.branchtalk.logging.Logger
import io.branchtalk.shared.infrastructure.KafkaSerialization.{ *, given }
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }
import io.branchtalk.shared.model.ShowPretty
import io.prometheus.client.CollectorRegistry

// Utilities for connecting to database and events buses through Resources.

final class DomainModule[Event: AvroEncoder: AvroDecoder, InternalEvent: AvroEncoder: AvroDecoder] {

  def setupReads[F[_]: Async](
    domainConfig: DomainModule.Config,
    logger:       Logger[F],
    registry:     CollectorRegistry
  ): Resource[F, Reads.Infrastructure[F, Event]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.databaseReads).transactor(logger, registry)
      consumerStreamBuilder = ConsumerStream.fromConfigs[F, Event](domainConfig.publishedEventBus)
    } yield Reads.Infrastructure(transactor, consumerStreamBuilder)

  def setupWrites[F[_]: Async: Dispatcher](
    domainConfig: DomainModule.Config,
    logger:       Logger[F],
    registry:     CollectorRegistry
  ): Resource[F, Writes.Infrastructure[F, Event, InternalEvent]] =
    for {
      transactor <- new PostgresDatabase(domainConfig.databaseWrites).transactor(logger, registry)
      internalProducer       = KafkaEventBus.producer[F, InternalEvent](domainConfig.internalEventBus)
      internalConsumerStream = ConsumerStream.fromConfigs[F, InternalEvent](domainConfig.internalEventBus)
      producer               = KafkaEventBus.producer[F, Event](domainConfig.publishedEventBus)
      consumerStream         = ConsumerStream.fromConfigs[F, Event](domainConfig.publishedEventBus)
      cache <- Cache.fromConfigs[F, Event](domainConfig.internalEventBus)
    } yield Writes.Infrastructure(
      transactor,
      internalProducer,
      internalConsumerStream(domainConfig.internalConsumer),
      producer,
      consumerStream,
      cache
    )
}
object DomainModule {

  def apply[Event: AvroEncoder: AvroDecoder, InternalEvent: AvroEncoder: AvroDecoder]: DomainModule[
    Event,
    InternalEvent
  ] = new DomainModule[Event, InternalEvent]

  type Name = Name.Type
  object Name extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(domainName: Name): Some[String] = Some(domainName.unwrap)

    given ConfigReader[Name] = summon[ConfigReader[String]].emapString("Name")(make)
    given Show[Name]         = unsafeMakeF[Show](Show[String])
  }

  final case class Config(
    name:              Name,
    databaseReads:     PostgresDatabase.Config,
    databaseWrites:    PostgresDatabase.Config,
    publishedEventBus: KafkaEventBus.BusConfig,
    internalEventBus:  KafkaEventBus.BusConfig,
    consumers:         Map[String, KafkaEventBus.ConsumerConfig]
  ) derives ConfigReader,
        ShowPretty {

    // assumes that each config has to have this field
    def internalConsumer: KafkaEventBus.ConsumerConfig = consumers("internal")
  }
}
