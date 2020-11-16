package io.branchtalk.shared.models

import scala.annotation.unused

/*
 * Prepends B to tuple A, so that you can build a tuple incrementally and don't end up with a nested tuple monstrosity
 */
trait TuplePrepender[A, B] {
  type Out
  def prepend(a:  A, b: B): Out
  def revert(out: Out): (A, B)
}

object TuplePrepender extends TuplePrependerLowPriorityImplicit {

  @inline def apply[A, B](implicit tp: TuplePrepender[A, B]): TuplePrepender[A, B] = tp

  type Aux[A, B, C] = TuplePrepender[A, B] { type Out = C } // scalastyle:ignore structural.type

  // scalastyle:off

  // unrolled, because it only handles 22 cases, so why use slow shapeless?
  implicit def prepender2[A, B, Add]: TuplePrepender.Aux[(A, B), Add, (Add, A, B)] =
    new TuplePrepender[(A, B), Add] {
      type Out = (Add, A, B)
      def prepend(tuple: (A, B), add: Add): Out = tuple match {
        case (a, b) =>
          (add, a, b)
      }
      def revert(tuple: Out): ((A, B), Add) = tuple match {
        case (add, a, b) =>
          ((a, b), add)
      }
    }

  implicit def prepender3[A, B, C, Add]: TuplePrepender.Aux[(A, B, C), Add, (Add, A, B, C)] =
    new TuplePrepender[(A, B, C), Add] {
      type Out = (Add, A, B, C)
      def prepend(tuple: (A, B, C), add: Add): Out = tuple match {
        case (a, b, c) =>
          (add, a, b, c)
      }
      def revert(tuple: Out): ((A, B, C), Add) = tuple match {
        case (add, a, b, c) =>
          ((a, b, c), add)
      }
    }

  implicit def prepender4[A, B, C, D, Add]: TuplePrepender.Aux[(A, B, C, D), Add, (Add, A, B, C, D)] =
    new TuplePrepender[(A, B, C, D), Add] {
      type Out = (Add, A, B, C, D)
      def prepend(tuple: (A, B, C, D), add: Add): Out = tuple match {
        case (a, b, c, d) =>
          (add, a, b, c, d)
      }
      def revert(tuple: Out): ((A, B, C, D), Add) = tuple match {
        case (add, a, b, c, d) =>
          ((a, b, c, d), add)
      }
    }

  implicit def prepender5[A, B, C, D, E, Add]: TuplePrepender.Aux[(A, B, C, D, E), Add, (Add, A, B, C, D, E)] =
    new TuplePrepender[(A, B, C, D, E), Add] {
      type Out = (Add, A, B, C, D, E)
      def prepend(tuple: (A, B, C, D, E), add: Add): Out = tuple match {
        case (a, b, c, d, e) =>
          (add, a, b, c, d, e)
      }
      def revert(tuple: Out): ((A, B, C, D, E), Add) = tuple match {
        case (add, a, b, c, d, e) =>
          ((a, b, c, d, e), add)
      }
    }

  implicit def prepender6[A, B, C, D, E, F, Add]: TuplePrepender.Aux[(A, B, C, D, E, F), Add, (Add, A, B, C, D, E, F)] =
    new TuplePrepender[(A, B, C, D, E, F), Add] {
      type Out = (Add, A, B, C, D, E, F)
      def prepend(tuple: (A, B, C, D, E, F), add: Add): Out = tuple match {
        case (a, b, c, d, e, f) =>
          (add, a, b, c, d, e, f)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F), Add) = tuple match {
        case (add, a, b, c, d, e, f) =>
          ((a, b, c, d, e, f), add)
      }
    }

  implicit def prepender7[A, B, C, D, E, F, G, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G),
    Add,
    (Add, A, B, C, D, E, F, G)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G), Add] {
      type Out = (Add, A, B, C, D, E, F, G)
      def prepend(tuple: (A, B, C, D, E, F, G), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g) => (add, a, b, c, d, e, f, g)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G), Add) = tuple match {
        case (add, a, b, c, d, e, f, g) => ((a, b, c, d, e, f, g), add)
      }
    }

  implicit def prepender8[A, B, C, D, E, F, G, H, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H),
    Add,
    (Add, A, B, C, D, E, F, G, H)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H)
      def prepend(tuple: (A, B, C, D, E, F, G, H), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h) => (add, a, b, c, d, e, f, g, h)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h) => ((a, b, c, d, e, f, g, h), add)
      }
    }

  implicit def prepender9[A, B, C, D, E, F, G, H, I, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I),
    Add,
    (Add, A, B, C, D, E, F, G, H, I)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i) => (add, a, b, c, d, e, f, g, h, i)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i) => ((a, b, c, d, e, f, g, h, i), add)
      }
    }

  implicit def prepender10[A, B, C, D, E, F, G, H, I, J, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j) => (add, a, b, c, d, e, f, g, h, i, j)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j) => ((a, b, c, d, e, f, g, h, i, j), add)
      }
    }

  implicit def prepender11[A, B, C, D, E, F, G, H, I, J, K, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k) => (add, a, b, c, d, e, f, g, h, i, j, k)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k) => ((a, b, c, d, e, f, g, h, i, j, k), add)
      }
    }

  implicit def prepender12[A, B, C, D, E, F, G, H, I, J, K, L, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l) => (add, a, b, c, d, e, f, g, h, i, j, k, l)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l) => ((a, b, c, d, e, f, g, h, i, j, k, l), add)
      }
    }

  implicit def prepender13[A, B, C, D, E, F, G, H, I, J, K, L, M, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m) => (add, a, b, c, d, e, f, g, h, i, j, k, l, m)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m) => ((a, b, c, d, e, f, g, h, i, j, k, l, m), add)
      }
    }

  implicit def prepender14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n) => (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n) => ((a, b, c, d, e, f, g, h, i, j, k, l, m, n), add)
      }
    }

  implicit def prepender15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) => (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) => ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o), add)
      }
    }

  implicit def prepender16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) =>
          (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p), add)
      }
    }

  implicit def prepender17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) =>
          (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q), add)
      }
    }

  implicit def prepender18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) =>
          (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r), add)
      }
    }

  implicit def prepender19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) =>
          (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s), add)
      }
    }

  implicit def prepender20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) =>
          (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t), add)
      }
    }

  implicit def prepender21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, Add]: TuplePrepender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U),
    Add,
    (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)
  ] =
    new TuplePrepender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U), Add] {
      type Out = (Add, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)
      def prepend(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) =>
          (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U), Add) = tuple match {
        case (add, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u), add)
      }
    }

  // scalastyle:on
}

trait TuplePrependerLowPriorityImplicit extends TuplePrependedEvenLowerPriorityImplicit {

  implicit def prependUnit[A]: TuplePrepender.Aux[Unit, A, A] = new TuplePrepender[Unit, A] {
    type Out = A

    def prepend(unit: Unit, a: A): Out = a

    def revert(a: Out): (Unit, A) = ((), a)
  }
}

trait TuplePrependedEvenLowerPriorityImplicit {

  implicit def appendNonTuple[A, B](implicit
    @unused ev: Refute[IsTuple[A]]
  ): TuplePrepender.Aux[A, B, (B, A)] = new TuplePrepender[A, B] {
    type Out = (B, A)
    def prepend(a: A, b: B): Out = (b, a)
    def revert(c:  Out): (A, B) = c.swap
  }
}
