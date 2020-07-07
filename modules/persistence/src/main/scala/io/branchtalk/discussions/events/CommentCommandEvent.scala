package io.branchtalk.discussions.events

import cats.implicits._
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.models.{ Comment, Post }
import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty, Updatable }
import io.branchtalk.users.models.User

@Semi(FastEq, ShowPretty) sealed trait CommentCommandEvent extends ADT
object CommentCommandEvent {

  @Semi(FastEq, ShowPretty) final case class Create(
    id:            ID[Comment],
    authorID:      ID[User],
    commentedPost: ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]],
    createdAt:     CreationTime
  ) extends CommentCommandEvent

  @Semi(FastEq, ShowPretty) final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content],
    modifiedAt: ModificationTime
  ) extends CommentCommandEvent

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentCommandEvent
}
