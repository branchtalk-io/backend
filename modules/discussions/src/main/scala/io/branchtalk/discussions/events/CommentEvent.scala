package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.scalaland.catnip.Semi
import io.branchtalk.discussions.model.{ Comment, Post, User }
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._
import io.branchtalk.ADT

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait CommentEvent extends ADT
object CommentEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Created(
    id:        ID[Comment],
    authorID:  ID[User],
    postID:    ID[Post],
    content:   Comment.Content,
    replyTo:   Option[ID[Comment]],
    createdAt: CreationTime
  ) extends CommentEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Updated(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content],
    modifiedAt: ModificationTime
  ) extends CommentEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Deleted(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restored(
    id:       ID[Comment],
    editorID: ID[User]
  ) extends CommentEvent
}
