package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Channel, Comment, Post, User }
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait CommentEvent extends ADT
object CommentEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Created(
    id:        ID[Comment],
    authorID:  ID[User],
    channelID: ID[Channel],
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
