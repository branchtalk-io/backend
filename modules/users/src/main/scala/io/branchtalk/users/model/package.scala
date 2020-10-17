package io.branchtalk.users

import cats.{ Eq, Show }
import io.estatico.newtype.macros.newtype

package object model {

  @newtype final case class Permissions(set: Set[Permission]) {

    def append(permission: Permission): Permissions = Permissions(set + permission)
    def remove(permission: Permission): Permissions = Permissions(set - permission)

    def allow(permissions:     Permission*): Boolean     = permissions.forall(set)
    def intersect(permissions: Permissions): Permissions = Permissions(set intersect permissions.set)
  }
  object Permissions {
    def unapply(permissions: Permissions): Option[Set[Permission]] = permissions.set.some

    def empty: Permissions = Permissions(Set.empty)

    implicit val show:  Show[Permissions] = (t: Permissions) => s"Permissions(${t.set.mkString(", ")})"
    implicit val order: Eq[Permissions]   = (x: Permissions, y: Permissions) => x.set === y.set
  }
}
