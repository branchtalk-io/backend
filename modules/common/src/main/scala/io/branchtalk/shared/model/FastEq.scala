package io.branchtalk.shared.model

import cats.Eq
import magnolia1._

import scala.language.experimental.macros

// Custom implementation of Eq which relies on Magnolia for derivation as opposed to Kittens' version.
trait FastEq[T] extends Eq[T]
object FastEq extends FastEqLowLevel {
  type Typeclass[T] = FastEq[T]

  def join[T](caseClass: ReadOnlyCaseClass[Typeclass, T]): Typeclass[T] =
    (x, y) => caseClass.parameters.forall(p => p.typeclass.eqv(p.dereference(x), p.dereference(y)))

  def split[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    (x, y) => sealedTrait.split(x)(sub => sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.cast(x), sub.cast(y)))

  def semi[T]: Typeclass[T] = macro Magnolia.gen[T]
}

trait FastEqLowLevel { self: FastEq.type =>

  implicit def liftEq[T](implicit normalEq: Eq[T]): FastEq[T] =
    (x: T, y: T) => normalEq.eqv(x, y)
}
