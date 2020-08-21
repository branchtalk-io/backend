package io.branchtalk.shared

import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.pureconfig._
import fs2.{ Pipe, Stream }
import fs2.kafka.{ CommittableConsumerRecord, Deserializer, ProducerResult }
import io.branchtalk.shared.models.UUID
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import pureconfig._

package object infrastructure {

  type EventBusProducer[F[_], Event] = Pipe[F, (UUID, Event), ProducerResult[UUID, Event, Unit]]
  type EventBusConsumer[F[_], Event] = Stream[F, CommittableConsumerRecord[F, UUID, Event]]

  type SafeDeserializer[F[_], Event] = Deserializer[F, DeserializationError Either Event]
  object SafeDeserializer {
    def apply[F[_], Event](implicit sd: SafeDeserializer[F, Event]): SafeDeserializer[F, Event] = sd
  }

  @newtype final case class DomainName(value: NonEmptyString)
  object DomainName {
    implicit val configReader: ConfigReader[DomainName] = ConfigReader[NonEmptyString].coerce
  }

  @newtype final case class DatabaseURL(value: NonEmptyString)
  object DatabaseURL {
    implicit val configReader: ConfigReader[DatabaseURL] = ConfigReader[NonEmptyString].coerce
  }
  @newtype final case class DatabaseUsername(value: NonEmptyString)
  object DatabaseUsername {
    implicit val configReader: ConfigReader[DatabaseUsername] = ConfigReader[NonEmptyString].coerce
  }
  @newtype final case class DatabasePassword(value: NonEmptyString) {
    override def toString: String = "[PASSWORD]"
  }
  object DatabasePassword {
    implicit val configReader: ConfigReader[DatabasePassword] = ConfigReader[NonEmptyString].coerce
  }
  @newtype final case class DatabaseSchema(value: NonEmptyString)
  object DatabaseSchema {
    implicit val configReader: ConfigReader[DatabaseSchema] = ConfigReader[NonEmptyString].coerce
  }
  @newtype final case class DatabaseDomain(value: NonEmptyString)
  object DatabaseDomain {
    implicit val configReader: ConfigReader[DatabaseDomain] = ConfigReader[NonEmptyString].coerce
  }
  @newtype final case class DatabaseConnectionPool(value: Int Refined Positive)
  object DatabaseConnectionPool {
    implicit val configReader: ConfigReader[DatabaseConnectionPool] = ConfigReader[Int Refined Positive].coerce
  }
  @newtype final case class DatabaseMigrationOnStart(value: Boolean)
  object DatabaseMigrationOnStart {
    implicit val configReader: ConfigReader[DatabaseMigrationOnStart] = ConfigReader[Boolean].coerce
  }

  @newtype final case class Topic(value: NonEmptyString)
  object Topic {
    implicit val configReader: ConfigReader[Topic] = ConfigReader[NonEmptyString].coerce
  }
}
