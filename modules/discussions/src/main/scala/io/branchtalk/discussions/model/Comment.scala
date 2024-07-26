package io.branchtalk.discussions.model

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

  export CommentCommands.{ Create, Delete, Downvote, Restore, RevokeVote, Update, Upvote }

  export CommentProperties.{
    Content,
    ControversialScore,
    Downvotes,
    NestingLevel,
    RepliesNr,
    Sorting,
    TotalScore,
    Upvotes
  }
}
