package io.branchtalk.discussions.model

import java.net.URI

import cats.effect.Sync
import cats.{ Order, Show }
import enumeratum.*
import enumeratum.EnumEntry.Hyphencase
import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.given // top-level AvroCodec[URI] etc. for newtype codecs

final case class Post(
  id:   ID[Post],
  data: Post.Data
) derives FastEq,
      ShowPretty
object Post {

  final case class Data(
    authorID:           ID[User],
    channelID:          ID[Channel],
    urlTitle:           Post.UrlTitle,
    title:              Post.Title,
    content:            Post.Content,
    createdAt:          CreationTime,
    lastModifiedAt:     Option[ModificationTime],
    commentsNr:         Post.CommentsNr,
    upvotes:            Post.Upvotes,
    downvotes:          Post.Downvotes,
    totalScore:         Post.TotalScore,
    controversialScore: Post.ControversialScore
  ) derives FastEq,
        ShowPretty

  final case class Create(
    authorID:  ID[User],
    channelID: ID[Channel],
    title:     Post.Title,
    content:   Post.Content
  ) derives FastEq,
        ShowPretty

  final case class Update(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content]
  ) derives FastEq,
        ShowPretty

  final case class Delete(
    id:       ID[Post],
    editorID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class Restore(
    id:       ID[Post],
    editorID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class Upvote(
    id:      ID[Post],
    voterID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class Downvote(
    id:      ID[Post],
    voterID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class RevokeVote(
    id:      ID[Post],
    voterID: ID[User]
  ) derives FastEq,
        ShowPretty

  type UrlTitle = UrlTitle.Type
  object UrlTitle extends Newtype[String] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(name: UrlTitle): Some[String] = Some(name.unwrap)

    given Show[UrlTitle]  = unsafeMakeF[Show](Show[String])
    given Order[UrlTitle] = unsafeMakeF[Order](Order[String])
  }

  type Title = Title.Type
  object Title extends Newtype[String] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(name: Title): Some[String] = Some(name.unwrap)

    given Show[Title]  = unsafeMakeF[Show](Show[String])
    given Order[Title] = unsafeMakeF[Order](Order[String])
  }

  type URL = URL.Type
  object URL extends Newtype[URI] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec
    def unapply(name: URL): Some[URI] = Some(name.unwrap)

    @SuppressWarnings(Array("org.wartremover.warts.ToString")) // false warning - URI overrides toString
    given Show[URL]  = _.unwrap.toString
    given Order[URL] = unsafeMakeF[Order](Order.fromComparable[URI])
  }

  type Text = Text.Type
  object Text extends Newtype[String] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec
    def unapply(name: Text): Some[String] = Some(name.unwrap)

    given Show[Text]  = unsafeMakeF[Show](Show[String]) // without wrapper because it lives only within Context.Text
    given Order[Text] = unsafeMakeF[Order](Order[String])
  }

  enum Content derives FastEq, ShowPretty {
    case Url(url: Post.URL)
    case Text(text: Post.Text)
  }
  object Content {

    // concrete single instance (so `Updatable[Post.Content]` resolves an AvroEncoder/Decoder and avoids Kindlings'
    // buggy structural derivation of the generic Updatable enum)
    given AvroCodec[Content] = AvroSupport.avroCodec(using AvroEncoder.derived, AvroDecoder.derived)

    enum Type extends EnumEntry, Hyphencase derives FastEq, ShowPretty {
      case Url
      case Text
    }

    type Raw = Raw.Type
    object Raw extends Newtype[String] {
      given AvroCodec[Raw.Type] = AvroSupport.newtypeCodec
      def unapply(name: Raw): Some[String] = Some(name.unwrap)

      given Show[Raw]  = unsafeMakeF[Show](Show[String])
      given Order[Raw] = unsafeMakeF[Order](Order[String])
    }

    object Tupled {
      def apply(contentType: Type, contentText: Raw): Content = contentType match {
        case Type.Url  => Content.Url(Post.URL.unsafeMake(URI.create(contentText.unwrap)))
        case Type.Text => Content.Text(Post.Text.unsafeMake(contentText.unwrap))
      }

      @SuppressWarnings(Array("org.wartremover.warts.ToString")) // false warning - URI overrides toString
      def unpack(content: Content): (Type, Raw) = content match {
        case Content.Url(url)   => Type.Url -> Raw.unsafeMake(url.unwrap.toString)
        case Content.Text(text) => Type.Text -> Raw.unsafeMake(text.unwrap)
      }

      def unapply(content: Content): Some[(Type, Raw)] = Some(unpack(content))
    }
  }

  type CommentsNr = CommentsNr.Type
  object CommentsNr extends Newtype[Int] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec

    override inline def validate(input: Int): Boolean = input >= 0

    def unapply(downvotes: CommentsNr): Some[Int] = Some(downvotes.unwrap)

    given Show[CommentsNr]  = unsafeMakeF[Show](Show[Int])
    given Order[CommentsNr] = unsafeMakeF[Order](Order[Int])
  }

  type Upvotes = Upvotes.Type
  object Upvotes extends Newtype[Int] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec

    override inline def validate(input: Int): Boolean = input >= 0

    def unapply(downvotes: Upvotes): Some[Int] = Some(downvotes.unwrap)

    given Show[Upvotes]  = unsafeMakeF[Show](Show[Int])
    given Order[Upvotes] = unsafeMakeF[Order](Order[Int])
  }

  type Downvotes = Downvotes.Type
  object Downvotes extends Newtype[Int] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec

    override inline def validate(input: Int): Boolean = input >= 0

    def unapply(downvotes: Downvotes): Some[Int] = Some(downvotes.unwrap)

    given Show[Downvotes]  = unsafeMakeF[Show](Show[Int])
    given Order[Downvotes] = unsafeMakeF[Order](Order[Int])
  }

  type TotalScore = TotalScore.Type
  object TotalScore extends Newtype[Int] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec
    def unapply(content: TotalScore): Some[Int] = Some(content.unwrap)

    given Show[TotalScore]  = unsafeMakeF[Show](Show[Int])
    given Order[TotalScore] = unsafeMakeF[Order](Order[Int])
  }

  type ControversialScore = ControversialScore.Type
  object ControversialScore extends Newtype[Int] {
    given AvroCodec[Type] = AvroSupport.newtypeCodec

    override inline def validate(input: Int): Boolean = input >= 0

    def unapply(controversialScore: ControversialScore): Some[Int] = Some(controversialScore.unwrap)

    given Show[ControversialScore]  = unsafeMakeF[Show](Show[Int])
    given Order[ControversialScore] = unsafeMakeF[Order](Order[Int])
  }

  enum Sorting derives FastEq, ShowPretty {
    case Newest
    case Hottest
    case Controversial
  }
}
