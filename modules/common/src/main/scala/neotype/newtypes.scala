package neotype

import cats.data.NonEmptyList
import cats.effect.Sync

// TODO: PR to upstream
abstract class NewtypeT[A] { self =>
  opaque type Type[B] = A

  def validate(input: A): Boolean | String = true

  inline def apply[B](inline input:     A):  Type[B]       = instance[B].apply(input)
  inline def applyAll[B](inline values: A*): List[Type[B]] = instance[B].applyAll(values*)

  final def make[B](input: A): Either[String, Type[B]] = validate(input) match
    case true  => Right(input)
    case false => Left("Validation Failed")
    case message: String => Left(message)

  inline def unwrap[B](inline input:            Type[B]): A          = input
  inline def unsafeMake[B](inline input:        A):       Type[B]    = input
  inline def unsafeMakeF[F[_], B](inline input: F[A]):    F[Type[B]] = input

  transparent inline given instance[B]: Newtype.WithType[A, Type[B]] = new Newtype[A] {
    override def validate(input: A): Boolean | String = self.validate(input)
  }.asInstanceOf[Newtype.WithType[A, Type[B]]]
}

// TODO: PR to upstream
abstract class NewtypeF[F[_]] { self =>
  opaque type Type[A] = F[A]

  def validate[A](input: F[A]): Boolean | String = true

  inline def apply[A](inline input:     F[A]):  Type[A]       = instance[A].apply(input)
  inline def applyAll[A](inline values: F[A]*): List[Type[A]] = instance[A].applyAll(values*)

  final def make[A](input: F[A]): Either[String, Type[A]] = validate(input) match
    case true  => Right(input)
    case false => Left("Validation Failed")
    case message: String => Left(message)

  inline def unwrap[A](inline input:            Type[A]): F[A]       = input
  inline def unsafeMake[A](inline input:        F[A]):    Type[A]    = input
  inline def unsafeMakeF[G[_], A](inline input: G[F[A]]): G[Type[A]] = input

  transparent inline given instance[A]: Newtype.WithType[F[A], Type[A]] = new Newtype[F[A]] {
    override def validate(input: F[A]): Boolean | String = self.validate(input)
  }.asInstanceOf[Newtype.WithType[F[A], Type[A]]]
}
