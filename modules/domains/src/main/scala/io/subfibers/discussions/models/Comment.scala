package io.subfibers.discussions.models

import io.estatico.newtype.macros.newtype
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.User

final case class Comment(
  id:             ID[Comment],
  authorID:       ID[User],
  commentedPost:  ID[Post],
  content:        CommentContent,
  replyTo:        Option[ID[Comment]],
  nestingLevel:   NestingLevel,
  createdAt:      CreationTime,
  lastModifiedAt: Option[ModificationTime]
)
object Comment {

  final case class Create(
    authorID:      ID[User],
    commentedPost: ID[Post],
    content:       CommentContent,
    replyTo:       Option[ID[Comment]]
  )

  final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Option[CommentContent]
  )

  @newtype final case class Delete(value: ID[Comment])
}
