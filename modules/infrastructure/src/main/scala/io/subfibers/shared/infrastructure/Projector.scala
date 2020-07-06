package io.subfibers.shared.infrastructure

import cats.implicits._
import cats.Monoid
import fs2._

trait Projector[F[_], -I, +O] extends Pipe[F, I, O] {
  def contramap[I2](f: I2 => I): Projector[F, I2, O] = (_: Stream[F, I2]).map(f).through(apply)
  def map[O2](f:       O => O2): Projector[F, I, O2] = (_: Stream[F, I]).through(apply).map(f)
}
object Projector {

  def lift[F[_], I, O](pipe: Pipe[F, I, O]): Projector[F, I, O] = (i: Stream[F, I]) => pipe(i)

  implicit def monoid[F[_], I, O]: Monoid[Projector[F, I, O]] = new Monoid[Projector[F, I, O]] {
    override def empty: Projector[F, I, O] = lift(Monoid[Pipe[F, I, O]].empty)
    override def combine(x: Projector[F, I, O], y: Projector[F, I, O]): Projector[F, I, O] =
      lift(Monoid[Pipe[F, I, O]].combine(x, y))
  }
}
