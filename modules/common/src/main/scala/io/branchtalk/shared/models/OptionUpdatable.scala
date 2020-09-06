package io.branchtalk.shared.models

import cats.{ Eq, Functor }

import scala.annotation.nowarn

sealed trait OptionUpdatable[+A] {

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
  @nowarn("cat=unused")
  implicit val functor: Functor[OptionUpdatable] = cats.derived.semi.functor[OptionUpdatable]
}
