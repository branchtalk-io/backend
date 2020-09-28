package io.branchtalk.users

import cats.{ Eq, Show }
import io.estatico.newtype.macros.newtype

package object model {

  @newtype final case class Permissions(value: Set[Permission])
  object Permissions {

    implicit val show:  Show[Permissions] = (t: Permissions) => s"Permissions(${t.value.mkString(", ")})"
    implicit val order: Eq[Permissions]   = (x: Permissions, y: Permissions) => x.value === y.value
  }

  @newtype final case class Settings(value: Set[Setting])
  object Settings {

    implicit val show:  Show[Settings] = (t: Settings) => s"Settings(${t.value.mkString(", ")})"
    implicit val order: Eq[Settings]   = (x: Settings, y: Settings) => x.value === y.value
  }
}
