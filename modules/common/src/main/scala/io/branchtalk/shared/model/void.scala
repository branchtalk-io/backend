package io.branchtalk.shared.model

import scala.annotation.unused

/** Explicitly mark value as unused, because it's e.g. returned from a mutable operation, and we care only about
  * side-effect.
  */
inline def void[A](@unused value: A): Unit = ()
