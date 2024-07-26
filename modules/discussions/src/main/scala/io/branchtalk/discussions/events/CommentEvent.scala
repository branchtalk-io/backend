package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.discussions.model.{ Channel, Comment, Post, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.*

sealed trait CommentEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object CommentEvent {

  final case class Created(
    id:            ID[Comment],
    authorID:      ID[User],
    channelID:     ID[Channel],
    postID:        ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]],
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends CommentEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Updated(
    id:            ID[Comment],
    editorID:      ID[User],
    newContent:    Updatable[Comment.Content],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends CommentEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Deleted(
    id:            ID[Comment],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Restored(
    id:            ID[Comment],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Upvoted(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Downvoted(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class VoteRevoked(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
