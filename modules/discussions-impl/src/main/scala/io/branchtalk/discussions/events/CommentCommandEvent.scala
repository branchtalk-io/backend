package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Channel, Comment, Post, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait CommentCommandEvent extends ADT
object CommentCommandEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Create(
    id:            ID[Comment],
    authorID:      ID[User],
    channelID:     ID[Channel],
    postID:        ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]],
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Update(
    id:            ID[Comment],
    editorID:      ID[User],
    newContent:    Updatable[Comment.Content],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Delete(
    id:            ID[Comment],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restore(
    id:            ID[Comment],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Upvote(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Downvote(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class RevokeVote(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent
}
