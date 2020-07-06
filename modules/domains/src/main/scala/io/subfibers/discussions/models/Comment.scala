package io.subfibers.discussions.models

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models._
import io.subfibers.users.models.User

@Semi(Eq, ShowPretty) final case class Comment(
  id:   ID[Comment],
  data: Comment.Data
)
object Comment extends CommentProperties with CommentCommands {

  @Semi(Eq, ShowPretty) final case class Data(
    authorID:       ID[User],
    commentedPost:  ID[Post],
    content:        Comment.Content,
    replyTo:        Option[ID[Comment]],
    nestingLevel:   Comment.NestingLevel,
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  )
}
