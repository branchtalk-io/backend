package io.subfibers.discussions.events

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.discussions.models.{ Comment, Post }
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime, Updatable }
import io.subfibers.users.models.User
import io.subfibers.ADT
import io.subfibers.shared.derivation.ShowPretty

@Semi(Eq, ShowPretty) sealed trait CommentEvent extends ADT
object CommentEvent {

  @Semi(Eq, ShowPretty) final case class Created(
    id:            ID[Comment],
    authorID:      ID[User],
    commentedPost: ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]],
    createdAt:     CreationTime
  ) extends CommentEvent

  @Semi(Eq, ShowPretty) final case class Updated(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content],
    modifiedAt: ModificationTime
  ) extends CommentEvent

  @Semi(Eq, ShowPretty) final case class Deleted(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentEvent
}
