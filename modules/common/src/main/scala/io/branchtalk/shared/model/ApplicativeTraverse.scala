package io.branchtalk.shared.model

import cats.{ Applicative, Eval, Traverse }

// because, apparently, you cannot have both at once since both extends Functor
trait ApplicativeTraverse[F[_]] extends Applicative[F] with Traverse[F]
object ApplicativeTraverse {

  def semi[F[_]: Applicative: Traverse]: ApplicativeTraverse[F] = {
    val a = Applicative[F]
    val t = Traverse[F]
    new ApplicativeTraverse[F] {
      override def pure[A](x: A): F[A] = a.pure(x)

      override def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] = a.ap(ff)(fa)

      override def traverse[G[_], A, B](fa: F[A])(f: A => G[B])(implicit G: Applicative[G]): G[F[B]] = t.traverse(fa)(f)

      override def foldLeft[A, B](fa: F[A], b: B)(f: (B, A) => B): B = t.foldLeft(fa, b)(f)

      override def foldRight[A, B](fa: F[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = t.foldRight(fa, lb)(f)
    }
  }
}
