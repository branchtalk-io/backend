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

  @newtype final case class Email(value: String Refined MatchesRegex["(.+)@(.+)"])
  object Email {
    def parse[F[_]: Sync](string: String): F[Email] =
      ParseRefined[F].parse[MatchesRegex["(.+)@(.+)"]](string).map(Email.apply)

    implicit val show:  Show[Email]  = (t: Email) => s"Email(${t.value.value.show})"
    implicit val order: Order[Email] = (x: Email, y: Email) => x.value.value compareTo y.value.value
  }

  @newtype final case class Name(value: NonEmptyString)
  object Name {
    def parse[F[_]: Sync](string: String): F[Name] =
      ParseRefined[F].parse[NonEmpty](string).map(Name.apply)

    implicit val show:  Show[Name]  = (t: Name) => s"User.Name(${t.value.value.show})"
    implicit val order: Order[Name] = (x: Name, y: Name) => x.value.value compareTo y.value.value
  }

  @newtype final case class Description(value: String)
  object Description {

    implicit val show: Show[Description] = (t: Description) => s"Email(${t.value.show})"
    implicit val eq:   Eq[Description]   = (x: Description, y: Description) => x.value === y.value
  }
}
