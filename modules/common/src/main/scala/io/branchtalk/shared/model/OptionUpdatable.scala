package io.branchtalk.shared.model

import cats.{ Applicative, Traverse }
import io.scalaland.catnip.Semi

import scala.annotation.nowarn

@Semi(ShowPretty, FastEq) sealed trait OptionUpdatable[+A] {

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
@nowarn("cat=unused") // macros
object OptionUpdatable {
  final case class Set[+A](value: A) extends OptionUpdatable[A]
  case object Erase extends OptionUpdatable[Nothing]
  case object Keep extends OptionUpdatable[Nothing]

  def setFromOption[A](option: Option[A]): OptionUpdatable[A] = option.fold[OptionUpdatable[A]](Erase)(Set(_))

  private val applicative: Applicative[OptionUpdatable] = new Applicative[OptionUpdatable] {
    override def pure[A](a:   A): OptionUpdatable[A] = Set(a)
    override def ap[A, B](ff: OptionUpdatable[A => B])(fa: OptionUpdatable[A]): OptionUpdatable[B] = (ff, fa) match {
      case (Set(f), Set(a)) => Set(f(a))
      case (Erase, _)       => Erase
      case (_, Erase)       => Erase
      case _                => Keep
    }
  }
  private val traverse:     Traverse[OptionUpdatable]            = cats.derived.semiauto.traverse[OptionUpdatable]
  implicit val appTraverse: ApplicativeTraverse[OptionUpdatable] = ApplicativeTraverse.semi(applicative, traverse)
}
