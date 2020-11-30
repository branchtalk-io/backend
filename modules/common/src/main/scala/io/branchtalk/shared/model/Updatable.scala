package io.branchtalk.shared.model

import cats.{ Applicative, Traverse }
import io.branchtalk.ADT
import io.scalaland.catnip.Semi

import scala.annotation.nowarn

@Semi(ShowPretty, FastEq) sealed trait Updatable[+A] extends ADT {

  def fold[B](set: A => B, keep: => B): B = this match {
    case Updatable.Set(value) => set(value)
    case Updatable.Keep       => keep
  }

  def toOption: Option[A] = this match {
    case Updatable.Set(value) => Some(value)
    case Updatable.Keep       => None
  }
}
@nowarn("cat=unused") // macros
object Updatable {
  final case class Set[+A](value: A) extends Updatable[A]
  case object Keep extends Updatable[Nothing]

  private val applicative: Applicative[Updatable] = new Applicative[Updatable] {
    override def pure[A](a:   A): Updatable[A] = Set(a)
    override def ap[A, B](ff: Updatable[A => B])(fa: Updatable[A]): Updatable[B] = (ff, fa) match {
      case (Set(f), Set(a)) => Set(f(a))
      case _                => Keep
    }
  }
  private val traverse:     Traverse[Updatable]            = cats.derived.semiauto.traverse[Updatable]
  implicit val appTraverse: ApplicativeTraverse[Updatable] = ApplicativeTraverse.semi(applicative, traverse)
}
