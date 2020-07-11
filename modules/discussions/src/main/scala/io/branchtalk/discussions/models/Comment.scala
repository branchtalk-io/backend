package io.branchtalk.discussions.models

import cats.implicits._
import io.scalaland.catnip.Semi
import io.branchtalk.shared.models._

@Semi(FastEq, ShowPretty) final case class Comment(
  id:   ID[Comment],
  data: Comment.Data
)
object Comment extends CommentProperties with CommentCommands {

  @Semi(FastEq, ShowPretty) final case class Data(
    authorID:       ID[User],
    commentedPost:  ID[Post],
    content:        Comment.Content,
    replyTo:        Option[ID[Comment]],
    nestingLevel:   Comment.NestingLevel,
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  )
}
