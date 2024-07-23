package io.branchtalk.shared.model

import cats.Eq
import magnolia1._

// Custom implementation of Eq which relies on Magnolia for derivation as opposed to Kittens' version.
trait FastEq[T] extends Eq[T]
object FastEq extends Derivation[FastEq] with FastEqLowLevel {

  def join[T](caseClass: CaseClass[FastEq, T]): FastEq[T] =
    (x, y) => caseClass.parameters.forall(p => p.typeclass.eqv(p.deref(x), p.deref(y)))

  def split[T](sealedTrait: SealedTrait[FastEq, T]): FastEq[T] =
    (x, y) => sealedTrait.choose(x)(sub => sub.cast.isDefinedAt(y) && sub.typeclass.eqv(sub.cast(x), sub.cast(y)))
}

trait FastEqLowLevel { self: FastEq.type =>

  implicit def liftEq[T](implicit normalEq: Eq[T]): FastEq[T] =
    (x: T, y: T) => normalEq.eqv(x, y)
}
