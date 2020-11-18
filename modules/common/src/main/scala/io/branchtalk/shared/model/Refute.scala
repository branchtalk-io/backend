package io.branchtalk.shared.model

import scala.annotation.unused

// removes dependency on shapeless, mostly copy pasted
sealed class Refute[A]
object Refute {

  private val impl = new Refute[Nothing]

  /** This results in  ambigous implicits if there is implicit evidence of `T` */
  implicit def ambiguousIfPresent[T](implicit @unused ev: T): Refute[T] = impl.asInstanceOf[Refute[T]]

  /** This always declares an instance of `Refute`
    *
    * This instance will only be found when there is no evidence of `T`
    */
  implicit def refute[T](implicit dummy: DummyImplicit): Refute[T] = impl.asInstanceOf[Refute[T]]
}
