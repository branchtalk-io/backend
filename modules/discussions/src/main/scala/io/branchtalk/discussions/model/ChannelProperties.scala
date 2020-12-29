package io.branchtalk.discussions.model

import cats.{ Order, Show }
import cats.effect.Sync
import enumeratum._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.model._
import io.estatico.newtype.macros.newtype

trait ChannelProperties { self: Channel.type =>
  type UrlName     = ChannelProperties.UrlName
  type Name        = ChannelProperties.Name
  type Description = ChannelProperties.Description
  type Sorting     = ChannelProperties.Sorting
  val UrlName     = ChannelProperties.UrlName
  val Name        = ChannelProperties.Name
  val Description = ChannelProperties.Description
  val Sorting     = ChannelProperties.Sorting
}
object ChannelProperties {

  @newtype final case class UrlName(urlString: String Refined MatchesRegex[UrlName.Pattern])
  object UrlName {
    type Pattern = "[A-Za-z0-9_-]+"

    def unapply(urlName: UrlName): Option[String Refined MatchesRegex[Pattern]] = urlName.urlString.some
    def parse[F[_]: Sync](string: String): F[UrlName] =
      ParseRefined[F].parse[MatchesRegex[Pattern]](string).map(UrlName.apply)

    implicit val show:  Show[UrlName]  = Show.wrap(_.urlString.value)
    implicit val order: Order[UrlName] = Order.by(_.urlString.value)
  }

  @newtype final case class Name(nonEmptyString: NonEmptyString)
  object Name {
    def unapply(name: Name): Option[NonEmptyString] = name.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Name] =
      ParseRefined[F].parse[NonEmpty](string).map(Name.apply)

    implicit val show:  Show[Name]  = Show.wrap(_.nonEmptyString.value)
    implicit val order: Order[Name] = Order.by(_.nonEmptyString.value)
  }

  @newtype final case class Description(nonEmptyString: NonEmptyString)
  object Description {
    def unapply(description: Description): Option[NonEmptyString] = description.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Description] =
      ParseRefined[F].parse[NonEmpty](string).map(Description.apply)

    implicit val show: Show[Description]  = Show.wrap(_.nonEmptyString.value)
    implicit val eq:   Order[Description] = Order.by(_.nonEmptyString.value)
  }

  sealed trait Sorting extends EnumEntry
  object Sorting extends Enum[Sorting] {
    case object Newest extends Sorting
    case object Alphabetically extends Sorting

    val values = findValues
  }
}
