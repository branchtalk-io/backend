package io.branchtalk.discussions.model

import io.branchtalk.shared.model.*

final case class Post(
  id:   ID[Post],
  data: Post.Data
) derives FastEq, ShowPretty
object Post extends PostProperties with PostCommands {

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
  ) derives FastEq, ShowPretty
}
