package io.branchtalk.discussions.models

import cats.{ Eq, Order, Show }
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

trait ChannelProperties { self: Channel.type =>
  type UrlName     = ChannelProperties.UrlName
  type Name        = ChannelProperties.Name
  type Description = ChannelProperties.Description
}
object ChannelProperties {

  @newtype final case class UrlName(value: String Refined MatchesRegex["[A-Za-z0-9_-]+"])
  object UrlName {
    implicit val show:  Show[UrlName]  = (t: UrlName) => s"UrlName(${t.value.value.show})"
    implicit val order: Order[UrlName] = (x: UrlName, y: UrlName) => x.value.value compareTo y.value.value
  }

  @newtype final case class Name(value: NonEmptyString)
  object Name {
    implicit val show:  Show[Name]  = (t: Name) => s"Name(${t.value.value.show})"
    implicit val order: Order[Name] = (x: Name, y: Name) => x.value.value compareTo y.value.value
  }

  @newtype final case class Description(value: NonEmptyString)
  object Description {
    implicit val show: Show[Description] = (t: Description) => s"Description(${t.value.value.show})"
    implicit val eq:   Eq[Description]   = (x: Description, y: Description) => x.value.value === y.value.value
  }
}
