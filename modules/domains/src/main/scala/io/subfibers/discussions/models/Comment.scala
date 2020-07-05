package io.subfibers.discussions.models

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import io.estatico.newtype.macros.newtype
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.models.User

final case class Comment(
  id:             ID[Comment],
  authorID:       ID[User],
  commentedPost:  ID[Post],
  content:        Comment.Content,
  replyTo:        Option[ID[Comment]],
  nestingLevel:   Comment.NestingLevel,
  createdAt:      CreationTime,
  lastModifiedAt: Option[ModificationTime]
)
object Comment {

  @newtype final case class Content(value:      String)
  @newtype final case class NestingLevel(value: Int Refined NonNegative)

  final case class Create(
    authorID:      ID[User],
    commentedPost: ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]]
  )

  final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Option[Comment.Content]
  )

  @newtype final case class Delete(value: ID[Comment])
}
