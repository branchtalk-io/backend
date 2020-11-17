package io.branchtalk.shared.models

import cats.{ Applicative, Eq, Traverse }
import io.branchtalk.ADT

import scala.annotation.nowarn

sealed trait Updatable[+A] extends ADT {

  def map[B](f: A => B): Updatable[B] = this match {
    case Updatable.Set(value) => Updatable.Set(f(value))
    case Updatable.Keep       => Updatable.Keep
  }

  def fold[B](set: A => B, keep: => B): B = this match {
    case Updatable.Set(value) => set(value)
    case Updatable.Keep       => keep
  }

  def toOption: Option[A] = this match {
    case Updatable.Set(value) => Some(value)
    case Updatable.Keep       => None
  }
}
object Updatable {
  final case class Set[+A](value: A) extends Updatable[A]
  case object Keep extends Updatable[Nothing]

  implicit def show[A: ShowPretty]: ShowPretty[Updatable[A]] = ShowPretty.semi
  implicit def eq[A:   Eq]:         Eq[Updatable[A]]         = FastEq.semi
  implicit val applicative: Applicative[Updatable] = new Applicative[Updatable] {
    override def pure[A](a:   A): Updatable[A] = Set(a)
    override def ap[A, B](ff: Updatable[A => B])(fa: Updatable[A]): Updatable[B] = (ff, fa) match {
      case (Set(f), Set(a)) => Set(f(a))
      case _                => Keep
    }
  }
  @nowarn("cat=unused") // macros
  implicit val traverse: Traverse[Updatable] = cats.derived.semiauto.traverse[Updatable]
}
