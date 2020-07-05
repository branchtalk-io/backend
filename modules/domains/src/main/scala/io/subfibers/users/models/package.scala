package io.subfibers.users

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

package object models {

  @newtype final case class Username(value: NonEmptyString)
}
