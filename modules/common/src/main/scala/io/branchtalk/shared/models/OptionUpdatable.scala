package io.branchtalk.shared.models

import cats.{ Applicative, Eq, Traverse }

import scala.annotation.nowarn

sealed trait OptionUpdatable[+A] {

  def map[B](f: A => B): OptionUpdatable[B] = this match {
    case OptionUpdatable.Set(value) => OptionUpdatable.Set(f(value))
    case OptionUpdatable.Erase      => OptionUpdatable.Erase
    case OptionUpdatable.Keep       => OptionUpdatable.Keep
  }

  def fold[B](set: A => B, keep: => B, erase: => B): B = this match {
    case OptionUpdatable.Set(value) => set(value)
    case OptionUpdatable.Erase      => keep
    case OptionUpdatable.Keep       => erase
  }

  def toOptionEither: Option[Either[Unit, A]] = this match {
    case OptionUpdatable.Set(value) => Some(Right(value))
    case OptionUpdatable.Erase      => Some(Left(()))
    case OptionUpdatable.Keep       => None
  }
}
object OptionUpdatable {
  final case class Set[+A](value: A) extends OptionUpdatable[A]
  case object Erase extends OptionUpdatable[Nothing]
  case object Keep extends OptionUpdatable[Nothing]

  def setFromOption[A](option: Option[A]): OptionUpdatable[A] = option.fold[OptionUpdatable[A]](Erase)(Set(_))

  implicit def show[A: ShowPretty]: ShowPretty[OptionUpdatable[A]] = ShowPretty.semi
  implicit def eq[A:   Eq]:         Eq[OptionUpdatable[A]]         = FastEq.semi
  implicit val applicative: Applicative[OptionUpdatable] = new Applicative[OptionUpdatable] {
    override def pure[A](a:   A): OptionUpdatable[A] = Set(a)
    override def ap[A, B](ff: OptionUpdatable[A => B])(fa: OptionUpdatable[A]): OptionUpdatable[B] = (ff, fa) match {
      case (Set(f), Set(a)) => Set(f(a))
      case (Erase, _)       => Erase
      case (_, Erase)       => Erase
      case _                => Keep
    }
  }
  @nowarn("cat=unused") // macros
  implicit val traverse: Traverse[OptionUpdatable] = cats.derived.semi.traverse[OptionUpdatable]
}
