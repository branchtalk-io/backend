package io.branchtalk.users

import cats.{ Eq, Show }
import io.estatico.newtype.macros.newtype

package object model {

  @newtype final case class Permissions(set: Set[Permission])
  object Permissions {
    def unapply(permissions: Permissions): Option[Set[Permission]] = permissions.set.some

    implicit val show:  Show[Permissions] = (t: Permissions) => s"Permissions(${t.set.mkString(", ")})"
    implicit val order: Eq[Permissions]   = (x: Permissions, y: Permissions) => x.set === y.set
  }
}
