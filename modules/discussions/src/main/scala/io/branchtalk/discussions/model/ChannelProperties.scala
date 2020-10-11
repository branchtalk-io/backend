package io.branchtalk.discussions.model

import cats.{ Eq, Order, Show }
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.models.ParseRefined
import io.estatico.newtype.macros.newtype

trait ChannelProperties { self: Channel.type =>
  type UrlName     = ChannelProperties.UrlName
  type Name        = ChannelProperties.Name
  type Description = ChannelProperties.Description
  val UrlName     = ChannelProperties.UrlName
  val Name        = ChannelProperties.Name
  val Description = ChannelProperties.Description
}
object ChannelProperties {

  @newtype final case class UrlName(urlString: String Refined MatchesRegex["[A-Za-z0-9_-]+"])
  object UrlName {
    def unapply(urlName: UrlName): Option[String Refined MatchesRegex["[A-Za-z0-9_-]+"]] = urlName.urlString.some
    def parse[F[_]: Sync](string: String): F[UrlName] =
      ParseRefined[F].parse[MatchesRegex["[A-Za-z0-9_-]+"]](string).map(UrlName.apply)

    implicit val show:  Show[UrlName]  = (t: UrlName) => s"UrlName(${t.urlString.value.show})"
    implicit val order: Order[UrlName] = (x: UrlName, y: UrlName) => x.urlString.value compareTo y.urlString.value
  }

  @newtype final case class Name(nonEmptyString: NonEmptyString)
  object Name {
    def unapply(name: Name): Option[NonEmptyString] = name.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Name] =
      ParseRefined[F].parse[NonEmpty](string).map(Name.apply)

    implicit val show:  Show[Name]  = (t: Name) => s"Name(${t.nonEmptyString.value.show})"
    implicit val order: Order[Name] = (x: Name, y: Name) => x.nonEmptyString.value compareTo y.nonEmptyString.value
  }

  @newtype final case class Description(nonEmptyString: NonEmptyString)
  object Description {
    def unapply(description: Description): Option[NonEmptyString] = description.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Description] =
      ParseRefined[F].parse[NonEmpty](string).map(Description.apply)

    implicit val show: Show[Description] = (t: Description) => s"Description(${t.nonEmptyString.value.show})"
    implicit val eq: Eq[Description] = (x: Description, y: Description) =>
      x.nonEmptyString.value === y.nonEmptyString.value
  }
}
