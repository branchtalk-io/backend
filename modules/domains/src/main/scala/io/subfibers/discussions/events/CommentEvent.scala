package io.subfibers.discussions.events

import io.subfibers.discussions.models.{ Comment, Post }
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.models.User
import io.subfibers.ADT

sealed trait CommentEvent extends ADT
object CommentEvent {

  sealed trait V1 extends CommentEvent
  object V1 {

    final case class CommentCreated(
      id:            ID[Comment],
      authorID:      ID[User],
      commentedPost: ID[Post],
      content:       Comment.Content,
      replyTo:       Option[ID[Comment]],
      createdAt:     CreationTime
    ) extends V1

    final case class CommentUpdated(
      id:             ID[Comment],
      editorID:       ID[User],
      newContent:     Option[Comment.Content],
      lastModifiedAt: ModificationTime
    ) extends V1

    final case class CommentDeleted(
      value: ID[Comment]
    ) extends V1
  }
}
