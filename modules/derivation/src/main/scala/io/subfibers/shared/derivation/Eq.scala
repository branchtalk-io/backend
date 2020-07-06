package io.subfibers.shared.derivation

import magnolia.{ Magnolia, ReadOnlyCaseClass, SealedTrait }

import scala.language.experimental.macros

object Eq {
  type Typeclass[T] = cats.Eq[T]

  def combine[T](caseClass: ReadOnlyCaseClass[Typeclass, T]): Typeclass[T] =
    (x, y) => caseClass.parameters.forall(p => p.typeclass.eqv(p.dereference(x), p.dereference(y)))

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    (x, y) => sealedTrait.dispatch(x)(sub => sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.cast(x), sub.cast(y)))

  def semi[T]: Typeclass[T] = macro Magnolia.gen[T]
}
