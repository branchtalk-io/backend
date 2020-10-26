package io.branchtalk.shared.models

import cats.{ Eq, Functor }
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
  @nowarn("cat=unused") // macros
  implicit val functor: Functor[Updatable] = cats.derived.semi.functor[Updatable]
}
