package io.branchtalk.shared.models

import scala.annotation.unused

/*
 * Prepends B to tuple A, so that you can build a tuple incrementally and don't end up with a nested tuple monstrosity
 */
trait TupleAppender[A, B] {
  type Out
  def append(a:   A, b: B): Out
  def revert(out: Out): (A, B)
}

object TupleAppender extends TupleAppenderLowPriorityImplicit {

  @inline def apply[A, B](implicit ta: TupleAppender[A, B]): TupleAppender[A, B] = ta

  type Aux[A, B, C] = TupleAppender[A, B] { type Out = C } // scalastyle:ignore structural.type

  // scalastyle:off

  // unrolled, because it only handles 22 cases, so why use slow shapeless?

  implicit def appender2[A, B, Add]: TupleAppender.Aux[(A, B), Add, (A, B, Add)] =
    new TupleAppender[(A, B), Add] {
      type Out = (A, B, Add)
      def append(tuple: (A, B), add: Add): Out = tuple match {
        case (a, b) => (a, b, add)
      }
      def revert(tuple: Out): ((A, B), Add) = tuple match {
        case (a, b, add) => ((a, b), add)
      }
    }

  implicit def appender3[A, B, C, Add]: TupleAppender.Aux[(A, B, C), Add, (A, B, C, Add)] =
    new TupleAppender[(A, B, C), Add] {
      type Out = (A, B, C, Add)
      def append(tuple: (A, B, C), add: Add): Out = tuple match {
        case (a, b, c) => (a, b, c, add)
      }
      def revert(tuple: Out): ((A, B, C), Add) = tuple match {
        case (a, b, c, add) => ((a, b, c), add)
      }
    }

  implicit def appender4[A, B, C, D, Add]: TupleAppender.Aux[(A, B, C, D), Add, (A, B, C, D, Add)] =
    new TupleAppender[(A, B, C, D), Add] {
      type Out = (A, B, C, D, Add)
      def append(tuple: (A, B, C, D), add: Add): Out = tuple match {
        case (a, b, c, d) => (a, b, c, d, add)
      }
      def revert(tuple: Out): ((A, B, C, D), Add) = tuple match {
        case (a, b, c, d, add) => ((a, b, c, d), add)
      }
    }

  implicit def appender5[A, B, C, D, E, Add]: TupleAppender.Aux[(A, B, C, D, E), Add, (A, B, C, D, E, Add)] =
    new TupleAppender[(A, B, C, D, E), Add] {
      type Out = (A, B, C, D, E, Add)
      def append(tuple: (A, B, C, D, E), add: Add): Out = tuple match {
        case (a, b, c, d, e) => (a, b, c, d, e, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E), Add) = tuple match {
        case (a, b, c, d, e, add) => ((a, b, c, d, e), add)
      }
    }

  implicit def appender6[A, B, C, D, E, F, Add]: TupleAppender.Aux[(A, B, C, D, E, F), Add, (A, B, C, D, E, F, Add)] =
    new TupleAppender[(A, B, C, D, E, F), Add] {
      type Out = (A, B, C, D, E, F, Add)
      def append(tuple: (A, B, C, D, E, F), add: Add): Out = tuple match {
        case (a, b, c, d, e, f) => (a, b, c, d, e, f, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F), Add) = tuple match {
        case (a, b, c, d, e, f, add) => ((a, b, c, d, e, f), add)
      }
    }

  implicit def appender7[A, B, C, D, E, F, G, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G),
    Add,
    (A, B, C, D, E, F, G, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G), Add] {
      type Out = (A, B, C, D, E, F, G, Add)
      def append(tuple: (A, B, C, D, E, F, G), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g) => (a, b, c, d, e, f, g, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G), Add) = tuple match {
        case (a, b, c, d, e, f, g, add) => ((a, b, c, d, e, f, g), add)
      }
    }

  implicit def appender8[A, B, C, D, E, F, G, H, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H),
    Add,
    (A, B, C, D, E, F, G, H, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H), Add] {
      type Out = (A, B, C, D, E, F, G, H, Add)
      def append(tuple: (A, B, C, D, E, F, G, H), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h) => (a, b, c, d, e, f, g, h, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, add) => ((a, b, c, d, e, f, g, h), add)
      }
    }

  implicit def appender9[A, B, C, D, E, F, G, H, I, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I),
    Add,
    (A, B, C, D, E, F, G, H, I, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i) => (a, b, c, d, e, f, g, h, i, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, add) => ((a, b, c, d, e, f, g, h, i), add)
      }
    }

  implicit def appender10[A, B, C, D, E, F, G, H, I, J, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J),
    Add,
    (A, B, C, D, E, F, G, H, I, J, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j) => (a, b, c, d, e, f, g, h, i, j, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, add) => ((a, b, c, d, e, f, g, h, i, j), add)
      }
    }

  implicit def appender11[A, B, C, D, E, F, G, H, I, J, K, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k) => (a, b, c, d, e, f, g, h, i, j, k, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, add) => ((a, b, c, d, e, f, g, h, i, j, k), add)
      }
    }

  implicit def appender12[A, B, C, D, E, F, G, H, I, J, K, L, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l) => (a, b, c, d, e, f, g, h, i, j, k, l, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, add) => ((a, b, c, d, e, f, g, h, i, j, k, l), add)
      }
    }

  implicit def appender13[A, B, C, D, E, F, G, H, I, J, K, L, M, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m) => (a, b, c, d, e, f, g, h, i, j, k, l, m, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, add) => ((a, b, c, d, e, f, g, h, i, j, k, l, m), add)
      }
    }

  implicit def appender14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, N, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n) => (a, b, c, d, e, f, g, h, i, j, k, l, m, n, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, add) => ((a, b, c, d, e, f, g, h, i, j, k, l, m, n), add)
      }
    }

  implicit def appender15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) => (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, add) => ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o), add)
      }
    }

  implicit def appender16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) => (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, add) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p), add)
      }
    }

  implicit def appender17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q) =>
          (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, add) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q), add)
      }
    }

  implicit def appender18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r) =>
          (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, add) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r), add)
      }
    }

  implicit def appender19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s) =>
          (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, add) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s), add)
      }
    }

  implicit def appender20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t) =>
          (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, add) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t), add)
      }
    }

  implicit def appender21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, Add]: TupleAppender.Aux[
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U),
    Add,
    (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, Add)
  ] =
    new TupleAppender[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U), Add] {
      type Out = (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, Add)
      def append(tuple: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U), add: Add): Out = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u) =>
          (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, add)
      }
      def revert(tuple: Out): ((A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U), Add) = tuple match {
        case (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, add) =>
          ((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u), add)
      }
    }

  // scalastyle:on
}

trait TupleAppenderLowPriorityImplicit extends TupleAppendedEvenLowerPriorityImplicit {

  implicit def appendUnit[A]: TupleAppender.Aux[Unit, A, A] = new TupleAppender[Unit, A] {
    type Out = A

    def append(unit: Unit, a: A): Out = a

    def revert(a: Out): (Unit, A) = ((), a)
  }
}

trait TupleAppendedEvenLowerPriorityImplicit {

  implicit def appendNonTuple[A, B](implicit
    @unused ev: Refute[IsTuple[A]]
  ): TupleAppender.Aux[A, B, (A, B)] = new TupleAppender[A, B] {
    type Out = (A, B)
    def append(a: A, b: B): Out = (a, b)
    def revert(c: Out): (A, B) = c
  }
}
