package io.branchtalk.shared.infrastructure

import cats.MonadThrow

extension [F[_]: MonadThrow, A](fa: F[A]) {
  // suppress complains from for-comprehension and case
  def withFilter(f: A => Boolean) = fa.flatMap { a =>
    if (f(a)) a.pure[F]
    else (new NoSuchElementException: Throwable).raiseError[F, A]
  }
}
