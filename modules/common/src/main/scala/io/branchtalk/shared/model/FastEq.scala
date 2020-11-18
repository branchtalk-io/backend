package io.branchtalk.shared.model

import cats.Eq
import magnolia.{ Magnolia, ReadOnlyCaseClass, SealedTrait }

import scala.language.experimental.macros

trait FastEq[T] extends Eq[T]
object FastEq extends FastEqLowLevel {
  type Typeclass[T] = FastEq[T]

  def combine[T](caseClass: ReadOnlyCaseClass[Typeclass, T]): Typeclass[T] =
    (x, y) => caseClass.parameters.forall(p => p.typeclass.eqv(p.dereference(x), p.dereference(y)))

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    (x, y) => sealedTrait.dispatch(x)(sub => sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.cast(x), sub.cast(y)))

  def semi[T]: Typeclass[T] = macro Magnolia.gen[T]
}

trait FastEqLowLevel { self: FastEq.type =>

  implicit def liftEq[T](implicit normalEq: Eq[T]): FastEq[T] =
    (x: T, y: T) => normalEq.eqv(x, y)
}
