package io.branchtalk.shared

import java.time.{ Instant, OffsetDateTime, ZoneId }
import java.time.format.DateTimeFormatter
import java.util.{ UUID => jUUID }

import cats.effect.{ Clock, Sync }
import cats.{ Eq, Functor, Order, Show }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

package object models {

  type UUID = jUUID
  object UUID {

    def apply(string: String Refined Uuid)(implicit uuidGenerator: UUIDGenerator): UUID =
      uuidGenerator(string)
    def create[F[_]: Sync](implicit uuidGenerator: UUIDGenerator): F[UUID] =
      uuidGenerator.create[F]
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[UUID] =
      uuidGenerator.parse[F](string)
  }

  @newtype final case class ID[+Entity](uuid: UUID)
  object ID {
    def unapply[Entity](entity: ID[Entity]): Option[UUID] = entity.uuid.some
    def create[F[_]: Sync, Entity](implicit uuidGenerator: UUIDGenerator): F[ID[Entity]] =
      UUID.create[F].map(ID[Entity])
    def parse[F[_]: Sync, Entity](string: String)(implicit uuidGenerator: UUIDGenerator): F[ID[Entity]] =
      UUID.parse[F](string).map(ID[Entity])

    implicit def show[Entity]:  Show[ID[Entity]]  = (id: ID[Entity]) => s"ID(${id.uuid.show})"
    implicit def order[Entity]: Order[ID[Entity]] = /*_*/ Order[UUID].coerce[Order[ID[Entity]]] /*_*/
  }

  // TODO: consider some custom clock(?)

  @newtype final case class CreationTime(offsetDateTime: OffsetDateTime)
  object CreationTime {
    def unapply(creationTime: CreationTime): Option[OffsetDateTime] = creationTime.offsetDateTime.some
    def now[F[_]: Functor: Clock]: F[CreationTime] =
      Clock[F]
        .realTime(scala.concurrent.duration.MILLISECONDS)
        .map(Instant.ofEpochMilli)
        .map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault()))
        .map(CreationTime(_))

    implicit val show: Show[CreationTime] =
      (t: CreationTime) => s"CreationTime(${DateTimeFormatter.ISO_INSTANT.format(t.offsetDateTime)})"
    implicit val order: Order[CreationTime] =
      (x: CreationTime, y: CreationTime) => x.offsetDateTime.compareTo(y.offsetDateTime)
  }
  @newtype final case class ModificationTime(offsetDateTime: OffsetDateTime)
  object ModificationTime {
    def unapply(modificationTime: ModificationTime): Option[OffsetDateTime] = modificationTime.offsetDateTime.some
    def now[F[_]: Functor: Clock]: F[ModificationTime] =
      Clock[F]
        .realTime(scala.concurrent.duration.MILLISECONDS)
        .map(Instant.ofEpochMilli)
        .map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault()))
        .map(ModificationTime(_))

    implicit val show: Show[ModificationTime] =
      (t: ModificationTime) => s"ModificationTime(${DateTimeFormatter.ISO_INSTANT.format(t.offsetDateTime)})"
    implicit val order: Order[ModificationTime] =
      (x: ModificationTime, y: ModificationTime) => x.offsetDateTime.compareTo(y.offsetDateTime)
  }

  @newtype final case class CreationScheduled[Entity](id: ID[Entity])
  object CreationScheduled {
    def unapply[Entity](creationScheduled: CreationScheduled[Entity]): Option[ID[Entity]] = creationScheduled.id.some

    implicit def show[Entity]: Show[CreationScheduled[Entity]] =
      (t: CreationScheduled[Entity]) => s"CreationScheduled(${t.id.show})"
    implicit def eq[Entity]: Eq[CreationScheduled[Entity]] = /*_*/ Eq[ID[Entity]].coerce /*_*/
  }
  @newtype final case class UpdateScheduled[Entity](id: ID[Entity])
  object UpdateScheduled {
    def unapply[Entity](updateScheduled: UpdateScheduled[Entity]): Option[ID[Entity]] = updateScheduled.id.some

    implicit def show[Entity]: Show[UpdateScheduled[Entity]] =
      (t: UpdateScheduled[Entity]) => s"UpdateScheduled(${t.id.show})"
    implicit def eq[Entity]: Eq[UpdateScheduled[Entity]] = /*_*/ Eq[ID[Entity]].coerce /*_*/
  }
  @newtype final case class DeletionScheduled[Entity](id: ID[Entity])
  object DeletionScheduled {
    def unapply[Entity](deletionScheduled: DeletionScheduled[Entity]): Option[ID[Entity]] = deletionScheduled.id.some

    implicit def show[Entity]: Show[DeletionScheduled[Entity]] =
      (t: DeletionScheduled[Entity]) => s"DeletionScheduled(${t.id.show})"
    implicit def eq[Entity]: Eq[DeletionScheduled[Entity]] = /*_*/ Eq[ID[Entity]].coerce /*_*/
  }
  @newtype final case class RestoreScheduled[Entity](id: ID[Entity])
  object RestoreScheduled {
    def unapply[Entity](restoreScheduled: RestoreScheduled[Entity]): Option[ID[Entity]] = restoreScheduled.id.some

    implicit def show[Entity]: Show[RestoreScheduled[Entity]] =
      (t: RestoreScheduled[Entity]) => s"RestoreScheduled(${t.id.show})"
    implicit def eq[Entity]: Eq[RestoreScheduled[Entity]] = /*_*/ Eq[ID[Entity]].coerce /*_*/
  }
}
