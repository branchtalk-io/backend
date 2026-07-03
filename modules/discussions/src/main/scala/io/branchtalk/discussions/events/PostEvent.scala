package io.branchtalk.discussions.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.discussions.model.{ Channel, Post, User }
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait PostEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object PostEvent {

  final case class Created(
    id:            ID[Post],
    authorID:      ID[User],
    channelID:     ID[Channel],
    urlTitle:      Post.UrlTitle,
    title:         Post.Title,
    content:       Post.Content,
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends PostEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Updated(
    id:            ID[Post],
    editorID:      ID[User],
    newUrlTitle:   Updatable[Post.UrlTitle],
    newTitle:      Updatable[Post.Title],
    newContent:    Updatable[Post.Content],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends PostEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Deleted(
    id:            ID[Post],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends PostEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Restored(
    id:            ID[Post],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends PostEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Upvoted(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Downvoted(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class VoteRevoked(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
