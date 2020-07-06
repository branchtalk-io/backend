package io.subfibers.discussions.models

import java.net.URI

import cats.{ Eq, Order, Show }
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.scalaland.catnip.Semi
import io.subfibers.ADT
import io.subfibers.shared.derivation.ShowPretty

trait PostProperties { self: Post.type =>
  type Title   = PostProperties.Title
  type URL     = PostProperties.URL
  type Text    = PostProperties.Text
  type Content = PostProperties.Content
}
object PostProperties {

  @newtype final case class Title(value: NonEmptyString)
  object Title {
    implicit val show:  Show[Title]  = (t: Title) => s"Title(${t.value.value.show})"
    implicit val order: Order[Title] = (x: Title, y: Title) => x.value.value compareTo y.value.value
  }

  @newtype final case class URL(value: URI)
  object URL {
    implicit val show:  Show[URL]  = (t: URL) => t.value.toString
    implicit val order: Order[URL] = (x: URL, y: URL) => x.value compareTo y.value
  }

  @newtype final case class Text(value: String)
  object Text {
    implicit val show:  Show[URL]  = (t: URL) => t.value.toString
    implicit val order: Order[URL] = (x: URL, y: URL) => x.value compareTo y.value
  }

  @Semi(Eq, ShowPretty) sealed trait Content extends ADT
  object Content {
    final case class Url(url:   URL) extends Content
    final case class Text(text: Text) extends Content
  }
}
