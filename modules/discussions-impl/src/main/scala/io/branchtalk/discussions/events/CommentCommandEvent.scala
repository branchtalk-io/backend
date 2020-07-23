package io.branchtalk.discussions.events

import com.sksamuel.avro4s.SchemaFor
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Comment, Post, User }
import io.branchtalk.shared.infrastructure.AvroSupport._
import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty, Updatable }

@Semi(FastEq, ShowPretty, SchemaFor) sealed trait CommentCommandEvent extends ADT
object CommentCommandEvent {

  @Semi(FastEq, ShowPretty) final case class Create(
    id:        ID[Comment],
    authorID:  ID[User],
    postID:    ID[Post],
    content:   Comment.Content,
    replyTo:   Option[ID[Comment]],
    createdAt: CreationTime
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

  @Semi(FastEq, ShowPretty) final case class Restore(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentCommandEvent
}
