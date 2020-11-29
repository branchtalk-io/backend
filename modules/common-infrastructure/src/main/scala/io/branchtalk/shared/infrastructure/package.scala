package io.branchtalk.shared

import cats.Show
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import fs2.{ Pipe, Stream }
import fs2.kafka.{ CommittableConsumerRecord, CommittableOffset, Deserializer, ProducerResult }
import io.branchtalk.shared.infrastructure.PureconfigSupport._
import io.branchtalk.shared.model.UUID
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

import scala.concurrent.duration.FiniteDuration

package object infrastructure {

  type EventBusProducer[F[_], Event] = Pipe[F, (UUID, Event), ProducerResult[UUID, Event, Unit]]
  type EventBusConsumer[F[_], Event] = Stream[F, CommittableConsumerRecord[F, UUID, Event]]
  type EventBusCommitter[F[_]]       = Pipe[F, CommittableOffset[F], Unit]

  type SafeDeserializer[F[_], Event] = Deserializer[F, DeserializationError Either Event]
  object SafeDeserializer {
    def apply[F[_], Event](implicit sd: SafeDeserializer[F, Event]): SafeDeserializer[F, Event] = sd
  }

  @newtype final case class DomainName(nonEmptyString: NonEmptyString)
  object DomainName {
    def unapply(domainName: DomainName): Option[NonEmptyString] = domainName.nonEmptyString.some

    implicit val configReader: ConfigReader[DomainName] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DomainName]         = _.nonEmptyString.value
  }

  @newtype final case class DatabaseURL(nonEmptyString: NonEmptyString)
  object DatabaseURL {
    def unapply(databaseURL: DatabaseURL): Option[NonEmptyString] = databaseURL.nonEmptyString.some

    implicit val configReader: ConfigReader[DatabaseURL] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabaseURL]         = _.nonEmptyString.value
  }
  @newtype final case class DatabaseUsername(nonEmptyString: NonEmptyString)
  object DatabaseUsername {
    def unapply(databaseUsername: DatabaseUsername): Option[NonEmptyString] = databaseUsername.nonEmptyString.some

    implicit val configReader: ConfigReader[DatabaseUsername] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabaseUsername]         = _.nonEmptyString.value
  }
  @newtype final case class DatabasePassword(nonEmptyString: NonEmptyString) {
    override def toString: String = "[PASSWORD]"
  }
  object DatabasePassword {
    def unapply(databasePassword: DatabasePassword): Option[NonEmptyString] = databasePassword.nonEmptyString.some

    implicit val configReader: ConfigReader[DatabasePassword] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabasePassword]         = _ => "[PASSWORD]"
  }
  @newtype final case class DatabaseSchema(nonEmptyString: NonEmptyString)
  object DatabaseSchema {
    def unapply(databaseSchema: DatabaseSchema): Option[NonEmptyString] = databaseSchema.nonEmptyString.some

    implicit val configReader: ConfigReader[DatabaseSchema] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabaseSchema]         = _.nonEmptyString.value
  }
  @newtype final case class DatabaseDomain(nonEmptyString: NonEmptyString)
  object DatabaseDomain {
    def unapply(databaseDomain: DatabaseDomain): Option[NonEmptyString] = databaseDomain.nonEmptyString.some

    implicit val configReader: ConfigReader[DatabaseDomain] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[DatabaseDomain]         = _.nonEmptyString.value
  }
  @newtype final case class DatabaseConnectionPool(positiveInt: Int Refined Positive)
  object DatabaseConnectionPool {
    def unapply(connectionPool: DatabaseConnectionPool): Option[Int Refined Positive] = connectionPool.positiveInt.some

    implicit val configReader: ConfigReader[DatabaseConnectionPool] = ConfigReader[Int Refined Positive].coerce
    implicit val show:         Show[DatabaseConnectionPool]         = _.positiveInt.toString
  }
  @newtype final case class DatabaseMigrationOnStart(bool: Boolean)
  object DatabaseMigrationOnStart {
    def unapply(migrationOnStart: DatabaseMigrationOnStart): Option[Boolean] = migrationOnStart.bool.some

    implicit val configReader: ConfigReader[DatabaseMigrationOnStart] = ConfigReader[Boolean].coerce
    implicit val show:         Show[DatabaseMigrationOnStart]         = Show[Boolean].coerce
  }

  @newtype final case class Topic(nonEmptyString: NonEmptyString)
  object Topic {
    def unapply(topic: Topic): Option[NonEmptyString] = topic.nonEmptyString.some

    implicit val configReader: ConfigReader[Topic] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[Topic]         = _.nonEmptyString.value
  }
  @newtype final case class ConsumerGroup(nonEmptyString: NonEmptyString)
  object ConsumerGroup {
    def unapply(consumerGroup: ConsumerGroup): Option[NonEmptyString] = consumerGroup.nonEmptyString.some

    implicit val configReader: ConfigReader[ConsumerGroup] = ConfigReader[NonEmptyString].coerce
    implicit val show:         Show[ConsumerGroup]         = _.nonEmptyString.value
  }
  @newtype final case class MaxCommitSize(positiveInt: Int Refined Positive)
  object MaxCommitSize {
    def unapply(maxCommitSize: MaxCommitSize): Option[Int Refined Positive] = maxCommitSize.positiveInt.some

    implicit val configReader: ConfigReader[MaxCommitSize] = ConfigReader[Int Refined Positive].coerce
    implicit val show:         Show[MaxCommitSize]         = _.positiveInt.toString
  }
  @newtype final case class MaxCommitTime(finiteDuration: FiniteDuration)
  object MaxCommitTime {
    def unapply(maxCommitTime: MaxCommitTime): Option[FiniteDuration] = maxCommitTime.finiteDuration.some

    implicit val configReader: ConfigReader[MaxCommitTime] = ConfigReader[FiniteDuration].coerce
    implicit val show:         Show[MaxCommitTime]         = _.finiteDuration.toString
  }
}
