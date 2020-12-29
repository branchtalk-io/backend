package io.branchtalk.users.model

import cats.{ Order, Show }
import cats.effect.Sync
import enumeratum.{ Enum, EnumEntry }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

trait UserProperties {
  type Email       = UserProperties.Email
  type Name        = UserProperties.Name
  type Description = UserProperties.Description
  type Filter      = UserProperties.Filter
  type Sorting     = UserProperties.Sorting
  val Email       = UserProperties.Email
  val Name        = UserProperties.Name
  val Description = UserProperties.Description
  val Filter      = UserProperties.Filter
  val Sorting     = UserProperties.Sorting
}
object UserProperties {

  @newtype final case class Email(emailString: String Refined MatchesRegex[Email.Pattern])
  object Email {
    type Pattern = "(.+)@(.+)"

    def unapply(email: Email): Option[String Refined MatchesRegex[Email.Pattern]] = email.emailString.some
    def parse[F[_]: Sync](string: String): F[Email] =
      ParseRefined[F].parse[MatchesRegex[Email.Pattern]](string).map(Email.apply)

    implicit val show:  Show[Email]  = Show.wrap(_.emailString.value)
    implicit val order: Order[Email] = Order.by(_.emailString.value)
  }

  @newtype final case class Name(nonEmptyString: NonEmptyString)
  object Name {
    def unapply(name: Name): Option[NonEmptyString] = name.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Name] =
      ParseRefined[F].parse[NonEmpty](string).map(Name.apply)

    implicit val show:  Show[Name]  = Show.wrap(_.nonEmptyString.value)
    implicit val order: Order[Name] = Order.by(_.nonEmptyString.value)
  }

  @newtype final case class Description(string: String)
  object Description {
    def unapply(description: Description): Option[String] = description.string.some

    implicit val show: Show[Description]  = Show.wrap(_.string)
    implicit val eq:   Order[Description] = Order[String].coerce
  }

  sealed trait Filter extends ADT
  object Filter {
    final case class HasPermission(permission: Permission) extends Filter
    final case class HasPermissions(permissions: Permissions) extends Filter
  }

  sealed trait Sorting extends EnumEntry
  object Sorting extends Enum[Sorting] {
    case object Newest extends Sorting
    case object NameAlphabetically extends Sorting
    case object EmailAlphabetically extends Sorting

    val values = findValues
  }
}
