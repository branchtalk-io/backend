package io.branchtalk.discussions.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.discussions.model.{ Channel, Comment, Post, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait CommentCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object CommentCommandEvent {

  final case class Create(
    id:            ID[Comment],
    authorID:      ID[User],
    channelID:     ID[Channel],
    postID:        ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]],
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends CommentCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Update(
    id:            ID[Comment],
    editorID:      ID[User],
    newContent:    Updatable[Comment.Content],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends CommentCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Delete(
    id:            ID[Comment],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Restore(
    id:            ID[Comment],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Upvote(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Downvote(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class RevokeVote(
    id:            ID[Comment],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends CommentCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
