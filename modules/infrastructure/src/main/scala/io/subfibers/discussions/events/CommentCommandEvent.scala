package io.subfibers.discussions.events

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.ADT
import io.subfibers.discussions.models.{ Comment, Post }
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime, Updatable }
import io.subfibers.users.models.User

@Semi(Eq, ShowPretty) sealed trait CommentCommandEvent extends ADT
object CommentCommandEvent {

  @Semi(Eq, ShowPretty) final case class Create(
    id:            ID[Comment],
    authorID:      ID[User],
    commentedPost: ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]],
    createdAt:     CreationTime
  ) extends CommentCommandEvent

  @Semi(Eq, ShowPretty) final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content],
    modifiedAt: ModificationTime
  ) extends CommentCommandEvent

  @Semi(Eq, ShowPretty) final case class Delete(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentCommandEvent
}
