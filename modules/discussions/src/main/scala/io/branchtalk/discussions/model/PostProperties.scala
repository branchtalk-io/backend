package io.branchtalk.discussions.model

import java.net.URI

import cats.effect.Sync
import cats.{ Order, Show }
import enumeratum._
import enumeratum.EnumEntry.Hyphencase
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.scalaland.catnip.Semi

trait PostProperties { self: Post.type =>
  type UrlTitle           = PostProperties.UrlTitle
  type Title              = PostProperties.Title
  type URL                = PostProperties.URL
  type Text               = PostProperties.Text
  type Content            = PostProperties.Content
  type CommentsNr         = PostProperties.CommentsNr
  type Upvotes            = PostProperties.Upvotes
  type Downvotes          = PostProperties.Downvotes
  type TotalScore         = PostProperties.TotalScore
  type ControversialScore = PostProperties.ControversialScore
  type Sorting            = PostProperties.Sorting
  val UrlTitle           = PostProperties.UrlTitle
  val Title              = PostProperties.Title
  val URL                = PostProperties.URL
  val Text               = PostProperties.Text
  val Content            = PostProperties.Content
  val CommentsNr         = PostProperties.CommentsNr
  val Upvotes            = PostProperties.Upvotes
  val Downvotes          = PostProperties.Downvotes
  val TotalScore         = PostProperties.TotalScore
  val ControversialScore = PostProperties.ControversialScore
  val Sorting            = PostProperties.Sorting
}
object PostProperties {

  @newtype final case class UrlTitle(nonEmptyString: NonEmptyString)
  object UrlTitle {
    def unapply(urlTitle: UrlTitle): Option[NonEmptyString] = urlTitle.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[UrlTitle] =
      ParseRefined[F].parse[NonEmpty](string).map(UrlTitle.apply)

    implicit val show:  Show[UrlTitle]  = Show.wrap(_.nonEmptyString.value)
    implicit val order: Order[UrlTitle] = Order.by(_.nonEmptyString.value)
  }

  @newtype final case class Title(nonEmptyString: NonEmptyString)
  object Title {
    def unapply(title: Title): Option[NonEmptyString] = title.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Title] =
      ParseRefined[F].parse[NonEmpty](string).map(Title.apply)

    implicit val show:  Show[Title]  = Show.wrap(_.nonEmptyString.value)
    implicit val order: Order[Title] = Order.by(_.nonEmptyString.value)
  }

  @newtype final case class URL(uri: URI)
  object URL {
    def unapply(url: URL): Option[URI] = url.uri.some

    implicit val show:  Show[URL]  = _.uri.toString // without wrapper because it lives only within Context.Url
    implicit val order: Order[URL] = Order.by[URL, URI](_.uri)(Order.fromComparable)
  }

  @newtype final case class Text(string: String)
  object Text {
    def unapply(text: Text): Option[String] = text.string.some

    implicit val show:  Show[Text]  = _.string // without wrapper because it lives only within Context.Text
    implicit val order: Order[Text] = Order.by(_.string)
  }

  @Semi(FastEq, ShowPretty) sealed trait Content extends ADT
  object Content {
    final case class Url(url: Post.URL) extends Content
    final case class Text(text: Post.Text) extends Content

    @Semi(FastEq, ShowPretty) sealed trait Type extends EnumEntry with Hyphencase
    object Type extends Enum[Type] {
      case object Url extends Type
      case object Text extends Type

      val values: IndexedSeq[Type] = findValues
    }
    @newtype final case class Raw(string: String)
    object Raw {
      def unapply(raw: Raw): Option[String] = raw.string.some

      implicit val show:  Show[Raw]  = Show.wrap(_.string)
      implicit val order: Order[Raw] = Order.by(_.string)
    }

    object Tupled {
      def apply(contentType: Type, contentText: Raw): Content = contentType match {
        case Type.Url  => Content.Url(Post.URL(URI.create(contentText.string)))
        case Type.Text => Content.Text(Post.Text(contentText.string))
      }

      def unpack(content: Content): (Type, Raw) = content match {
        case Content.Url(url)   => Type.Url -> Raw(url.uri.toString)
        case Content.Text(text) => Type.Text -> Raw(text.string)
      }

      def unapply(content: Content): Option[(Type, Raw)] = unpack(content).some
    }
  }

  @newtype final case class CommentsNr(toNonNegativeInt: Int Refined NonNegative)
  object CommentsNr {
    def unapply(commentsNr: CommentsNr): Option[Int Refined NonNegative] = commentsNr.toNonNegativeInt.some

    implicit val show:  Show[CommentsNr]  = Show.wrap(_.toNonNegativeInt.value)
    implicit val order: Order[CommentsNr] = Order.by(_.toNonNegativeInt.value)
  }

  @newtype final case class Upvotes(toNonNegativeInt: Int Refined NonNegative)
  object Upvotes {
    def unapply(upvotes: Upvotes): Option[Int Refined NonNegative] = upvotes.toNonNegativeInt.some

    implicit val show:  Show[Upvotes]  = Show.wrap(_.toNonNegativeInt.value)
    implicit val order: Order[Upvotes] = Order.by(_.toNonNegativeInt.value)
  }

  @newtype final case class Downvotes(toNonNegativeInt: Int Refined NonNegative)
  object Downvotes {
    def unapply(downvotes: Downvotes): Option[Int Refined NonNegative] = downvotes.toNonNegativeInt.some

    implicit val show:  Show[Downvotes]  = Show.wrap(_.toNonNegativeInt.value)
    implicit val order: Order[Downvotes] = Order.by(_.toNonNegativeInt.value)
  }

  @newtype final case class TotalScore(toInt: Int)
  object TotalScore {
    def unapply(totalScore: TotalScore): Option[Int] = totalScore.toInt.some

    implicit val show:  Show[TotalScore]  = Show.wrap(_.toInt)
    implicit val order: Order[TotalScore] = Order.by(_.toInt)
  }

  @newtype final case class ControversialScore(toNonNegativeInt: Int Refined NonNegative)
  object ControversialScore {
    def unapply(controversialScore: ControversialScore): Option[Int Refined NonNegative] =
      controversialScore.toNonNegativeInt.some

    implicit val show:  Show[ControversialScore]  = Show.wrap(_.toNonNegativeInt.value)
    implicit val order: Order[ControversialScore] = Order.by(_.toNonNegativeInt.value)
  }

  sealed trait Sorting extends EnumEntry
  object Sorting extends Enum[Sorting] {
    case object Newest extends Sorting
    case object TotalScore extends Sorting
    case object ControversialScore extends Sorting

    val values = findValues
  }
}
