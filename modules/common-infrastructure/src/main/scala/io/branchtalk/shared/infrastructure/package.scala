package io.branchtalk.shared

import cats.Show
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import fs2.{ Pipe, Stream }
import fs2.kafka.{ CommittableConsumerRecord, CommittableOffset, Deserializer, ProducerResult }
import io.branchtalk.shared.infrastructure.PureconfigSupport._
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import io.branchtalk.shared.model._
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

import scala.concurrent.duration.FiniteDuration

package object infrastructure {

  type EventBusProducer[F[_], Event] = Pipe[F, (UUID, Event), ProducerResult[Unit, UUID, Event]]
  type EventBusConsumer[F[_], Event] = Stream[F, CommittableConsumerRecord[F, UUID, Event]]
  type EventBusCommitter[F[_]]       = Pipe[F, CommittableOffset[F], Unit]

  type SafeDeserializer[F[_], Event] = Deserializer[F, DeserializationResult[Event]]
  object SafeDeserializer {
    @inline def apply[F[_], Event](implicit sd: SafeDeserializer[F, Event]): SafeDeserializer[F, Event] = sd
  }

  @newtype final case class DomainName(nonEmptyString: NonEmptyString)
  object DomainName {
    def unapply(domainName: DomainName): Some[NonEmptyString] = Some(domainName.nonEmptyString)

    implicit val configReader: ConfigReader[DomainName] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DomainName]         = Show.wrap(_.nonEmptyString.value)
  }

  @newtype final case class DatabaseURL(nonEmptyString: NonEmptyString)
  object DatabaseURL {
    def unapply(databaseURL: DatabaseURL): Some[NonEmptyString] = Some(databaseURL.nonEmptyString)

    implicit val configReader: ConfigReader[DatabaseURL] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabaseURL]         = Show.wrap(_.nonEmptyString.value)
  }
  @newtype final case class DatabaseUsername(nonEmptyString: NonEmptyString)
  object DatabaseUsername {
    def unapply(databaseUsername: DatabaseUsername): Some[NonEmptyString] = Some(databaseUsername.nonEmptyString)

    implicit val configReader: ConfigReader[DatabaseUsername] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabaseUsername]         = Show.wrap(_.nonEmptyString.value)
  }
  @newtype final case class DatabaseSchema(nonEmptyString: NonEmptyString)
  object DatabaseSchema {
    def unapply(databaseSchema: DatabaseSchema): Some[NonEmptyString] = Some(databaseSchema.nonEmptyString)

    implicit val configReader: ConfigReader[DatabaseSchema] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabaseSchema]         = Show.wrap(_.nonEmptyString.value)
  }
  @newtype final case class DatabaseDomain(nonEmptyString: NonEmptyString)
  object DatabaseDomain {
    def unapply(databaseDomain: DatabaseDomain): Some[NonEmptyString] = Some(databaseDomain.nonEmptyString)

    implicit val configReader: ConfigReader[DatabaseDomain] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabaseDomain]         = Show.wrap(_.nonEmptyString.value)
  }
  @newtype final case class DatabaseConnectionPool(positiveInt: Int Refined Positive)
  object DatabaseConnectionPool {
    def unapply(connectionPool: DatabaseConnectionPool): Some[Int Refined Positive] = Some(connectionPool.positiveInt)

    implicit val configReader: ConfigReader[DatabaseConnectionPool] = ConfigReader[Int Refined Positive].coerce
    implicit val show:         Show[DatabaseConnectionPool]         = Show.wrap(_.positiveInt.value)
  }
  @newtype final case class DatabaseMigrationOnStart(bool: Boolean)
  object DatabaseMigrationOnStart {
    def unapply(migrationOnStart: DatabaseMigrationOnStart): Some[Boolean] = Some(migrationOnStart.bool)

    implicit val configReader: ConfigReader[DatabaseMigrationOnStart] = ConfigReader[Boolean].coerce
    implicit val show:         Show[DatabaseMigrationOnStart]         = Show.wrap(_.bool)
  }

  @newtype final case class Topic(nonEmptyString: NonEmptyString)
  object Topic {
    def unapply(topic: Topic): Some[NonEmptyString] = Some(topic.nonEmptyString)

    implicit val configReader: ConfigReader[Topic] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[Topic]         = Show.wrap(_.nonEmptyString.value)
  }
  @newtype final case class ConsumerGroup(nonEmptyString: NonEmptyString)
  object ConsumerGroup {
    def unapply(consumerGroup: ConsumerGroup): Some[NonEmptyString] = Some(consumerGroup.nonEmptyString)

    implicit val configReader: ConfigReader[ConsumerGroup] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[ConsumerGroup]         = Show.wrap(_.nonEmptyString.value)
  }
  @newtype final case class MaxCommitSize(positiveInt: Int Refined Positive)
  object MaxCommitSize {
    def unapply(maxCommitSize: MaxCommitSize): Some[Int Refined Positive] = Some(maxCommitSize.positiveInt)

    implicit val configReader: ConfigReader[MaxCommitSize] = ConfigReader[Int Refined Positive].coerce
    implicit val show:         Show[MaxCommitSize]         = Show.wrap(_.positiveInt.value)
  }
  @newtype final case class MaxCommitTime(finiteDuration: FiniteDuration)
  object MaxCommitTime {
    def unapply(maxCommitTime: MaxCommitTime): Some[FiniteDuration] = Some(maxCommitTime.finiteDuration)

    implicit val configReader: ConfigReader[MaxCommitTime] = ConfigReader[FiniteDuration].coerce
    implicit val show:         Show[MaxCommitTime]         = Show.wrap(_.finiteDuration)
  }
}
