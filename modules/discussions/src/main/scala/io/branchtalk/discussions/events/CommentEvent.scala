package io.branchtalk.discussions.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.discussions.model.{ Channel, Comment, Post, User }
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait CommentEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
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
  ) extends CommentEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Updated(
    id:            ID[Comment],
    editorID:      ID[User],
    newContent:    Updatable[Comment.Content],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends CommentEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Deleted(
    id:            ID[Comment],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Restored(
    id:            ID[Comment],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Upvoted(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Downvoted(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class VoteRevoked(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
