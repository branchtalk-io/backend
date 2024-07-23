package io.branchtalk.shared.model

import cats.{ Applicative, Traverse }

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
  private val traverse:     Traverse[Updatable]            = cats.derived.semiauto.traverse[Updatable]
  implicit val appTraverse: ApplicativeTraverse[Updatable] = ApplicativeTraverse.derived(applicative, traverse)
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
  private val traverse: Traverse[OptionUpdatable] = cats.derived.semiauto.traverse[OptionUpdatable]
  given ApplicativeTraverse[OptionUpdatable] = ApplicativeTraverse.derived(applicative, traverse)
}
