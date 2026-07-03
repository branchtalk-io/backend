package io.branchtalk.shared.model

import cats.Eq
import hearth.kindlings.catsderivation.extensions.*

// Type class aliases backed by Kindlings' Hearth-based derivation (replaces the old Magnolia FastEq/ShowPretty).
// Keeping the `FastEq`/`ShowPretty` names lets existing `derives FastEq, ShowPretty` clauses stay untouched.

/** Recursive, fast `cats.Eq` derivation. */
type FastEq[A] = Eq[A]
object FastEq {
  inline def derived[A]: Eq[A] = Eq.derived[A]
  inline def apply[A](using ev: Eq[A]): Eq[A] = ev
}

/** Pretty, multi-line `cats.Show` derivation (a `cats.Show` with extra `showLines`). */
type ShowPretty[A] = hearth.kindlings.catsderivation.ShowPretty[A]
object ShowPretty {
  inline def derived[A]: ShowPretty[A] = hearth.kindlings.catsderivation.ShowPretty.derived[A]
  inline def apply[A](using ev: ShowPretty[A]): ShowPretty[A] = ev
}
