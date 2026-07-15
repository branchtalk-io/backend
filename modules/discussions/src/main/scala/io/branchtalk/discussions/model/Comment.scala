package io.branchtalk.discussions.model

import java.net.URI

import cats.effect.Sync
import cats.{ Order, Show }
import enumeratum.*
import enumeratum.EnumEntry.Hyphencase
import io.branchtalk.shared.model.*

final case class Comment(
  id:   ID[Comment],
  data: Comment.Data
) derives FastEq,
      ShowPretty
object Comment {

  final case class Data(
    authorID:           ID[User],
    channelID:          ID[Channel],
    postID:             ID[Post],
    content:            Comment.Content,
    replyTo:            Option[ID[Comment]],
    nestingLevel:       Comment.NestingLevel,
    createdAt:          CreationTime,
    lastModifiedAt:     Option[ModificationTime],
    repliesNr:          Comment.RepliesNr,
    upvotes:            Comment.Upvotes,
    downvores:          Comment.Downvotes,
    totalScore:         Comment.TotalScore,
    controversialScore: Comment.ControversialScore
  ) derives FastEq,
        ShowPretty

  final case class Create(
    authorID: ID[User],
    postID:   ID[Post],
    content:  Comment.Content,
    replyTo:  Option[ID[Comment]]
  ) derives FastEq,
        ShowPretty

  final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content]
  ) derives FastEq,
        ShowPretty

  final case class Delete(
    id:       ID[Comment],
    editorID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class Restore(
    id:       ID[Comment],
    editorID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class Upvote(
    id:      ID[Comment],
    voterID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class Downvote(
    id:      ID[Comment],
    voterID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class RevokeVote(
    id:      ID[Comment],
    voterID: ID[User]
  ) derives FastEq,
        ShowPretty

  type Content = Content.Type
  object Content extends Newtype[String] {
    def unapply(content: Content): Some[String] = Some(content.unwrap)

    given Show[Content]  = unsafeMakeF[Show](Show[String])
    given Order[Content] = unsafeMakeF[Order](Order[String])
  }

  type NestingLevel = NestingLevel.Type
  object NestingLevel extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input >= 0

    def unapply(nestingLevel: NestingLevel): Some[Int] = Some(nestingLevel.unwrap)

    given Show[NestingLevel]  = unsafeMakeF[Show](Show[Int])
    given Order[NestingLevel] = unsafeMakeF[Order](Order[Int])
  }

  type RepliesNr = RepliesNr.Type
  object RepliesNr extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input >= 0

    def unapply(repliesNr: RepliesNr): Some[Int] = Some(repliesNr.unwrap)

    given Show[RepliesNr]  = unsafeMakeF[Show](Show[Int])
    given Order[RepliesNr] = unsafeMakeF[Order](Order[Int])
  }

  type Upvotes = Upvotes.Type
  object Upvotes extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input >= 0

    def unapply(upvotes: Upvotes): Some[Int] = Some(upvotes.unwrap)

    given Show[Upvotes]  = unsafeMakeF[Show](Show[Int])
    given Order[Upvotes] = unsafeMakeF[Order](Order[Int])
  }

  type Downvotes = Downvotes.Type
  object Downvotes extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input >= 0

    def unapply(downvotes: Downvotes): Some[Int] = Some(downvotes.unwrap)

    given Show[Downvotes]  = unsafeMakeF[Show](Show[Int])
    given Order[Downvotes] = unsafeMakeF[Order](Order[Int])
  }

  type TotalScore = TotalScore.Type
  object TotalScore extends Newtype[Int] {
    def unapply(content: TotalScore): Some[Int] = Some(content.unwrap)

    given Show[TotalScore]  = unsafeMakeF[Show](Show[Int])
    given Order[TotalScore] = unsafeMakeF[Order](Order[Int])
  }

  type ControversialScore = ControversialScore.Type
  object ControversialScore extends Newtype[Int] {

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
