package io.branchtalk.users

import cats.{ Eq, Show }
import io.estatico.newtype.macros.newtype

package object model {

  @newtype final case class Permissions(set: Set[Permission])
  object Permissions {

    implicit val show:  Show[Permissions] = (t: Permissions) => s"Permissions(${t.set.mkString(", ")})"
    implicit val order: Eq[Permissions]   = (x: Permissions, y: Permissions) => x.set === y.set
  }
}
