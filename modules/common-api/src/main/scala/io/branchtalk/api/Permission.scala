package io.branchtalk.api

import io.branchtalk.ADT

sealed trait Permission extends ADT
object Permission {
  case object Mock extends Permission
}
