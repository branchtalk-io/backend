package io.subfibers.shared

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.{ UUID => jUUID }

import cats.effect.{ Clock, Sync }
import cats.implicits._
import cats.{ Eq, Functor, Order, Show }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

package object models {

  type UUID = jUUID
  object UUID {

    def apply(string: String Refined Uuid): UUID = jUUID.fromString(string.value)
    // TODO: use some UUIDGen type class which could use e.g. time-based UUID generation
    def create[F[_]: Sync]: F[UUID] = Sync[F].delay(jUUID.randomUUID())
    def parse[F[_]:  Sync](string: String): F[UUID] = Sync[F].delay(jUUID.fromString(string))
  }

  @newtype final case class ID[+Entity](value: UUID)
  object ID {
    implicit def show[Entity]: Show[ID[Entity]] = (id: ID[Entity]) => s"ID(${id.value.show})"
    implicit def eq[Entity]:   Eq[ID[Entity]]   = /*_*/ Eq[UUID].coerce[Eq[ID[Entity]]] /*_*/
  }

  // TODO: consider some custom clock(?)

  @newtype final case class CreationTime(value: Instant)
  object CreationTime {
    implicit val show: Show[CreationTime] =
      (t: CreationTime) => s"CreationTime(${DateTimeFormatter.ISO_INSTANT.format(t.value)})"
    implicit val order: Order[CreationTime] =
      (x: CreationTime, y: CreationTime) => x.value.compareTo(y.value)

    def now[F[_]: Functor: Clock]: F[CreationTime] =
      Clock[F].realTime(scala.concurrent.duration.MILLISECONDS).map(Instant.ofEpochMilli).map(CreationTime(_))
  }
  @newtype final case class ModificationTime(value: Instant)
  object ModificationTime {
    implicit val show: Show[ModificationTime] =
      (t: ModificationTime) => s"ModificationTime(${DateTimeFormatter.ISO_INSTANT.format(t.value)})"
    implicit val order: Order[ModificationTime] =
      (x: ModificationTime, y: ModificationTime) => x.value.compareTo(y.value)

    def now[F[_]: Functor: Clock]: F[ModificationTime] =
      Clock[F].realTime(scala.concurrent.duration.MILLISECONDS).map(Instant.ofEpochMilli).map(ModificationTime(_))
  }

  @newtype final case class CreationScheduled[Entity](id: ID[Entity])
  object CreationScheduled {
    implicit def show[Entity]: Show[CreationScheduled[Entity]] =
      (t: CreationScheduled[Entity]) => s"CreationScheduled(${t.id.show})"
    implicit def eq[Entity]: Eq[CreationScheduled[Entity]] = /*_*/ Eq[ID[Entity]].coerce /*_*/
  }
  @newtype final case class UpdateScheduled[Entity](id: ID[Entity])
  object UpdateScheduled {
    implicit def show[Entity]: Show[UpdateScheduled[Entity]] =
      (t: UpdateScheduled[Entity]) => s"UpdateScheduled(${t.id.show})"
    implicit def eq[Entity]: Eq[UpdateScheduled[Entity]] = /*_*/ Eq[ID[Entity]].coerce /*_*/
  }
  @newtype final case class DeletionScheduled[Entity](id: ID[Entity])
  object DeletionScheduled {
    implicit def show[Entity]: Show[DeletionScheduled[Entity]] =
      (t: DeletionScheduled[Entity]) => s"DeletionScheduled(${t.id.show})"
    implicit def eq[Entity]: Eq[DeletionScheduled[Entity]] = /*_*/ Eq[ID[Entity]].coerce /*_*/
  }
}
