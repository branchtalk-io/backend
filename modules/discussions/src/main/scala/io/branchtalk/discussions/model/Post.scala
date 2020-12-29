package io.branchtalk.discussions.model

import io.scalaland.catnip.Semi
import io.branchtalk.shared.model._

@Semi(FastEq, ShowPretty) final case class Post(
  id:   ID[Post],
  data: Post.Data
)
object Post extends PostProperties with PostCommands {

  @Semi(FastEq, ShowPretty) final case class Data(
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
  )
}
