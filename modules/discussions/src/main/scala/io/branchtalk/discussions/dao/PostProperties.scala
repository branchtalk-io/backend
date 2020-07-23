package io.branchtalk.discussions.dao

import java.net.URI

import cats.{ Order, Show }
import eu.timepit.refined.types.string.NonEmptyString
import enumeratum._
import enumeratum.EnumEntry.Hyphencase
import io.estatico.newtype.macros.newtype
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.shared.models.{ FastEq, ShowPretty }

trait PostProperties { self: Post.type =>
  type UrlTitle = PostProperties.UrlTitle
  type Title    = PostProperties.Title
  type URL      = PostProperties.URL
  type Text     = PostProperties.Text
  type Content  = PostProperties.Content
  val UrlTitle = PostProperties.UrlTitle
  val Title    = PostProperties.Title
  val URL      = PostProperties.URL
  val Text     = PostProperties.Text
  val Content  = PostProperties.Content
}
object PostProperties {

  @newtype final case class UrlTitle(value: NonEmptyString)
  object UrlTitle {
    implicit val show:  Show[UrlTitle]  = (t: UrlTitle) => s"Title(${t.value.value.show})"
    implicit val order: Order[UrlTitle] = (x: UrlTitle, y: UrlTitle) => x.value.value compareTo y.value.value
  }

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
    implicit val show:  Show[Text]  = (t: Text) => t.value
    implicit val order: Order[Text] = (x: Text, y: Text) => x.value compareTo y.value
  }

  @Semi(FastEq, ShowPretty) sealed trait Content extends ADT
  object Content {
    final case class Url(url:   Post.URL) extends Content
    final case class Text(text: Post.Text) extends Content

    @Semi(FastEq, ShowPretty) sealed trait Type extends EnumEntry with Hyphencase
    object Type extends Enum[Type] {
      case object Url extends Type
      case object Text extends Type

      override def values: IndexedSeq[Type] = findValues
    }
    @newtype final case class Raw(value: String)
    object Raw {
      implicit val show:  Show[Raw]  = (t: Raw) => t.value
      implicit val order: Order[Raw] = (x: Raw, y: Raw) => x.value compareTo y.value
    }

    object Tupled {
      def apply(contentType: Type, contentText: Raw): Content = contentType match {
        case Type.Url  => Content.Url(Post.URL(URI.create(contentText.value)))
        case Type.Text => Content.Text(Post.Text(contentText.value))
      }

      def unpack(content: Content): (Type, Raw) = content match {
        case Content.Url(url)   => Type.Url -> Raw(url.value.toString)
        case Content.Text(text) => Type.Text -> Raw(text.value)
      }

      def unapply(content: Content): Option[(Type, Raw)] = unpack(content).some
    }
  }
}
