package io.branchtalk.shared.models

import cats.Eq
import io.branchtalk.ADT

sealed trait Updatable[+A] extends ADT {

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
}
