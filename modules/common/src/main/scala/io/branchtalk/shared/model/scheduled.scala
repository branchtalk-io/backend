package io.branchtalk.shared.model

import cats.{ Eq, Show }
import neotype.*

type CreationScheduled[A] = CreationScheduled.Type[A]
object CreationScheduled extends NewtypeF[ID] {

  def unapply[Entity](creationScheduled: CreationScheduled[Entity]): Some[ID[Entity]] = Some(creationScheduled.unwrap)

  given [Entity]: Show[CreationScheduled[Entity]] = unsafeMakeF[Show, Entity](Show[ID[Entity]])
  given [Entity]: Eq[CreationScheduled[Entity]]   = unsafeMakeF[Eq, Entity](Eq[ID[Entity]])
}

type UpdateScheduled[A] = UpdateScheduled.Type[A]
object UpdateScheduled extends NewtypeF[ID] {

  def unapply[Entity](updateScheduled: UpdateScheduled[Entity]): Some[ID[Entity]] = Some(updateScheduled.unwrap)

  given [Entity]: Show[UpdateScheduled[Entity]] = unsafeMakeF[Show, Entity](Show[ID[Entity]])
  given [Entity]: Eq[UpdateScheduled[Entity]]   = unsafeMakeF[Eq, Entity](Eq[ID[Entity]])
}

type DeletionScheduled[A] = DeletionScheduled.Type[A]
object DeletionScheduled extends NewtypeF[ID] {

  def unapply[Entity](deletionScheduled: DeletionScheduled[Entity]): Some[ID[Entity]] = Some(deletionScheduled.unwrap)

  given [Entity]: Show[DeletionScheduled[Entity]] = unsafeMakeF[Show, Entity](Show[ID[Entity]])
  given [Entity]: Eq[DeletionScheduled[Entity]]   = unsafeMakeF[Eq, Entity](Eq[ID[Entity]])
}

type RestoreScheduled[A] = RestoreScheduled.Type[A]
object RestoreScheduled extends NewtypeF[ID] {

  def unapply[Entity](restoreScheduled: RestoreScheduled[Entity]): Some[ID[Entity]] = Some(restoreScheduled.unwrap)

  given [Entity]: Show[RestoreScheduled[Entity]] = unsafeMakeF[Show, Entity](Show[ID[Entity]])
  given [Entity]: Eq[RestoreScheduled[Entity]]   = unsafeMakeF[Eq, Entity](Eq[ID[Entity]])
}
