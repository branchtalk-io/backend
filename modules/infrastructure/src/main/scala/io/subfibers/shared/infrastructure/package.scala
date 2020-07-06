package io.subfibers.shared

import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import fs2.{ Pipe, Stream }
import fs2.kafka.{ CommittableConsumerRecord, ProducerResult }
import io.estatico.newtype.macros.newtype

package object infrastructure {

  @newtype final case class DomainName(value: NonEmptyString)

  type EventBusProducer[F[_], Key, Event] = Pipe[F, (Key, Event), ProducerResult[Key, Event, Unit]]
  type EventBusConsumer[F[_], Key, Event] = Stream[F, CommittableConsumerRecord[F, Key, Event]]

  @newtype final case class DatabaseURL(value:      NonEmptyString)
  @newtype final case class DatabaseUsername(value: NonEmptyString)
  @newtype final case class DatabasePassword(value: NonEmptyString) {
    override def toString: String = "[PASSWORD]"
  }
  @newtype final case class DatabaseSchema(value:           NonEmptyString)
  @newtype final case class DatabaseDomain(value:           NonEmptyString)
  @newtype final case class DatabaseConnectionPool(value:   Int Refined Positive)
  @newtype final case class DatabaseMigrationOnStart(value: Boolean)

  @newtype final case class Topic(value: NonEmptyString)
}
