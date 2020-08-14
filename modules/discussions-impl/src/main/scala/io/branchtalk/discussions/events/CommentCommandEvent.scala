package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Comment, Post, User }
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait CommentCommandEvent extends ADT
object CommentCommandEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Create(
    id:        ID[Comment],
    authorID:  ID[User],
    postID:    ID[Post],
    content:   Comment.Content,
    replyTo:   Option[ID[Comment]],
    createdAt: CreationTime
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content],
    modifiedAt: ModificationTime
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Delete(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restore(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentCommandEvent
}
