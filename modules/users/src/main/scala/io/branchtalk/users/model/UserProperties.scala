package io.branchtalk.users.model

import cats.{ Eq, Order, Show }
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.models.ParseRefined
import io.estatico.newtype.macros.newtype

trait UserProperties {
  type Email       = UserProperties.Email
  type Name        = UserProperties.Name
  type Description = UserProperties.Description
  val Email       = UserProperties.Email
  val Name        = UserProperties.Name
  val Description = UserProperties.Description
}
object UserProperties {

  // TODO: make it an ADT with new-confirmed-updated lifecycle
  @newtype final case class Email(string: String Refined MatchesRegex["(.+)@(.+)"])
  object Email {
    def unapply(email: Email): Option[String Refined MatchesRegex["(.+)@(.+)"]] = email.string.some
    def parse[F[_]: Sync](string: String): F[Email] =
      ParseRefined[F].parse[MatchesRegex["(.+)@(.+)"]](string).map(Email.apply)

    implicit val show:  Show[Email]  = (t: Email) => s"Email(${t.string.value.show})"
    implicit val order: Order[Email] = (x: Email, y: Email) => x.string.value compareTo y.string.value
  }

  @newtype final case class Name(string: NonEmptyString)
  object Name {
    def unapply(name: Name): Option[NonEmptyString] = name.string.some
    def parse[F[_]: Sync](string: String): F[Name] =
      ParseRefined[F].parse[NonEmpty](string).map(Name.apply)

    implicit val show:  Show[Name]  = (t: Name) => s"User.Name(${t.string.value.show})"
    implicit val order: Order[Name] = (x: Name, y: Name) => x.string.value compareTo y.string.value
  }

  @newtype final case class Description(string: String)
  object Description {
    def unapply(description: Description): Option[String] = description.string.some

    implicit val show: Show[Description] = (t: Description) => s"Email(${t.string.show})"
    implicit val eq:   Eq[Description]   = (x: Description, y: Description) => x.string === y.string
  }
}
