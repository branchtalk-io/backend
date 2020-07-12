package io.branchtalk.shared

import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.pureconfig._
import fs2.{ Pipe, Stream }
import fs2.kafka.{ CommittableConsumerRecord, ProducerResult }
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import pureconfig._

package object infrastructure {

  @newtype final case class DomainName(value: NonEmptyString)
  object DomainName {
    implicit val configReader: ConfigReader[DomainName] = ConfigReader[NonEmptyString].coerce[ConfigReader[DomainName]]
  }

  type EventBusProducer[F[_], Key, Event] = Pipe[F, (Key, Event), ProducerResult[Key, Event, Unit]]
  type EventBusConsumer[F[_], Key, Event] = Stream[F, CommittableConsumerRecord[F, Key, Event]]

  @newtype final case class DatabaseURL(value: NonEmptyString)
  object DatabaseURL {
    implicit val configReader: ConfigReader[DatabaseURL] =
      ConfigReader[NonEmptyString].coerce[ConfigReader[DatabaseURL]]
  }
  @newtype final case class DatabaseUsername(value: NonEmptyString)
  object DatabaseUsername {
    implicit val configReader: ConfigReader[DatabaseUsername] =
      ConfigReader[NonEmptyString].coerce[ConfigReader[DatabaseUsername]]
  }
  @newtype final case class DatabasePassword(value: NonEmptyString) {
    override def toString: String = "[PASSWORD]"
  }
  object DatabasePassword {
    implicit val configReader: ConfigReader[DatabasePassword] =
      ConfigReader[NonEmptyString].coerce[ConfigReader[DatabasePassword]]
  }
  @newtype final case class DatabaseSchema(value: NonEmptyString)
  object DatabaseSchema {
    implicit val configReader: ConfigReader[DatabaseSchema] =
      ConfigReader[NonEmptyString].coerce[ConfigReader[DatabaseSchema]]
  }
  @newtype final case class DatabaseDomain(value: NonEmptyString)
  object DatabaseDomain {
    implicit val configReader: ConfigReader[DatabaseDomain] =
      ConfigReader[NonEmptyString].coerce[ConfigReader[DatabaseDomain]]
  }
  @newtype final case class DatabaseConnectionPool(value: Int Refined Positive)
  object DatabaseConnectionPool {
    implicit val configReader: ConfigReader[DatabaseConnectionPool] =
      ConfigReader[Int Refined Positive].coerce[ConfigReader[DatabaseConnectionPool]]
  }
  @newtype final case class DatabaseMigrationOnStart(value: Boolean)
  object DatabaseMigrationOnStart {
    implicit val configReader: ConfigReader[DatabaseMigrationOnStart] =
      ConfigReader[Boolean].coerce[ConfigReader[DatabaseMigrationOnStart]]
  }

  @newtype final case class Topic(value: NonEmptyString)
  object Topic {
    implicit val configReader: ConfigReader[Topic] =
      ConfigReader[NonEmptyString].coerce[ConfigReader[Topic]]
  }
}
