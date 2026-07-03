package io.branchtalk.shared.model

import cats.{ Applicative, Eval, Traverse }

// Express the intent that something should be updated or not better than Option.
enum Updatable[+A] derives ShowPretty, FastEq {
  case Set(value: A)
  case Keep

  def fold[B](set: A => B, keep: => B): B = this match {
    case Set(value) => set(value)
    case Keep       => keep
  }

  def toOption: Option[A] = this match {
    case Set(value) => Some(value)
    case Keep       => None
  }
}
object Updatable {

  private val applicative: Applicative[Updatable] = new Applicative[Updatable] {
    override def pure[A](a: A): Updatable[A] = Set(a)
    override def ap[A, B](ff: Updatable[A => B])(fa: Updatable[A]): Updatable[B] = (ff, fa) match {
      case (Set(f), Set(a)) => Set(f(a))
      case _                => Keep
    }
  }
  // hand-written: Kindlings' Traverse derivation does not support the singleton `Keep` case
  private val traverse: Traverse[Updatable] = new Traverse[Updatable] {
    override def traverse[G[_]: Applicative, A, B](fa: Updatable[A])(f: A => G[B]): G[Updatable[B]] = fa match {
      case Set(value) => f(value).map(Set(_))
      case Keep       => Applicative[G].pure[Updatable[B]](Keep)
    }
    override def foldLeft[A, B](fa: Updatable[A], b: B)(f: (B, A) => B): B = fa match {
      case Set(value) => f(b, value)
      case Keep       => b
    }
    override def foldRight[A, B](fa: Updatable[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match {
      case Set(value) => f(value, lb)
      case Keep       => lb
    }
  }
  given ApplicativeTraverse[Updatable] = ApplicativeTraverse.derived(applicative, traverse)
}

// Express the intent that something should be updated, erased or kept better than Option[Either[Unit, *]].
enum OptionUpdatable[+A] derives ShowPretty, FastEq {
  case Set(value: A)
  case Erase
  case Keep

  def fold[B](set: A => B, keep: => B, erase: => B): B = this match {
    case Set(value) => set(value)
    case Erase      => keep
    case Keep       => erase
  }

  def toOptionEither: Option[Either[Unit, A]] = this match {
    case Set(value) => Some(Right(value))
    case Erase      => Some(Left(()))
    case Keep       => None
  }
}
object OptionUpdatable {

  def setFromOption[A](option: Option[A]): OptionUpdatable[A] = option.fold[OptionUpdatable[A]](Erase)(Set(_))

  private val applicative: Applicative[OptionUpdatable] = new Applicative[OptionUpdatable] {
    override def pure[A](a: A): OptionUpdatable[A] = Set(a)
    override def ap[A, B](ff: OptionUpdatable[A => B])(fa: OptionUpdatable[A]): OptionUpdatable[B] = (ff, fa) match {
      case (Set(f), Set(a)) => Set(f(a))
      case (Erase, _)       => Erase
      case (_, Erase)       => Erase
      case _                => Keep
    }
  }
  // hand-written: Kindlings' Traverse derivation does not support the singleton `Erase`/`Keep` cases
  private val traverse: Traverse[OptionUpdatable] = new Traverse[OptionUpdatable] {
    override def traverse[G[_]: Applicative, A, B](fa: OptionUpdatable[A])(f: A => G[B]): G[OptionUpdatable[B]] =
      fa match {
        case Set(value) => f(value).map(Set(_))
        case Erase      => Applicative[G].pure[OptionUpdatable[B]](Erase)
        case Keep       => Applicative[G].pure[OptionUpdatable[B]](Keep)
      }
    override def foldLeft[A, B](fa: OptionUpdatable[A], b: B)(f: (B, A) => B): B = fa match {
      case Set(value)   => f(b, value)
      case Erase | Keep => b
    }
    override def foldRight[A, B](fa: OptionUpdatable[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match {
      case Set(value)   => f(value, lb)
      case Erase | Keep => lb
    }
  }
  given ApplicativeTraverse[OptionUpdatable] = ApplicativeTraverse.derived(applicative, traverse)
}
