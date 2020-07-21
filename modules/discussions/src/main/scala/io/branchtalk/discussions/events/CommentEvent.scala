package io.branchtalk.discussions.events

import io.scalaland.catnip.Semi
import io.branchtalk.discussions.models.{ Comment, Post, User }
import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty, Updatable }
import io.branchtalk.ADT

@Semi(FastEq, ShowPretty) sealed trait CommentEvent extends ADT
object CommentEvent {

  @Semi(FastEq, ShowPretty) final case class Created(
    id:            ID[Comment],
    authorID:      ID[User],
    commentedPost: ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]],
    createdAt:     CreationTime
  ) extends CommentEvent

  @Semi(FastEq, ShowPretty) final case class Updated(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content],
    modifiedAt: ModificationTime
  ) extends CommentEvent

  @Semi(FastEq, ShowPretty) final case class Deleted(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentEvent
}
