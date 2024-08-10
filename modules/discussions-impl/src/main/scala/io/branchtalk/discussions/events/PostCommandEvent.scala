package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.discussions.model.{ Channel, Post, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait PostCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object PostCommandEvent {

  final case class Create(
    id:            ID[Post],
    authorID:      ID[User],
    channelID:     ID[Channel],
    urlTitle:      Post.UrlTitle,
    title:         Post.Title,
    content:       Post.Content,
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends PostCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Update(
    id:            ID[Post],
    editorID:      ID[User],
    newUrlTitle:   Updatable[Post.UrlTitle],
    newTitle:      Updatable[Post.Title],
    newContent:    Updatable[Post.Content],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends PostCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Delete(
    id:            ID[Post],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends PostCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Restore(
    id:            ID[Post],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends PostCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Upvote(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Downvote(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class RevokeVote(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
