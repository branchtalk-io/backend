package io

import java.time.Instant
import java.util.{ UUID => jUUID }

import cats.effect.{ Clock, Sync }
import cats.implicits._
import cats.Functor
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import io.estatico.newtype.macros.newtype

package object subfibers {

  type UUID = jUUID
  object UUID {

    def apply(string: String Refined Uuid): UUID = jUUID.fromString(string.value)
    def parse[F[_]:  Sync](string: String): F[UUID] = Sync[F].delay(jUUID.fromString(string))
    def random[F[_]: Sync]: F[UUID] = Sync[F].delay(jUUID.randomUUID())
  }

  @newtype final case class ID[+Entity](value: UUID)

  @newtype final case class CreationTime(value: Instant)
  object CreationTime {

    def now[F[_]: Functor: Clock]: F[CreationTime] =
      Clock[F].realTime(scala.concurrent.duration.MILLISECONDS).map(Instant.ofEpochMilli).map(CreationTime(_))
  }
  @newtype final case class ModificationTime(value: Instant)
  object ModificationTime {

    def now[F[_]: Functor: Clock]: F[CreationTime] =
      Clock[F].realTime(scala.concurrent.duration.MILLISECONDS).map(Instant.ofEpochMilli).map(CreationTime(_))
  }

  @newtype final case class CreationScheduled[Entity](id: ID[Entity])
  @newtype final case class UpdateScheduled[Entity](id:   ID[Entity])
  @newtype final case class DeletionScheduled[Entity](id: ID[Entity])
}
