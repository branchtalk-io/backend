package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Channel, Post, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait PostEvent extends ADT
object PostEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Created(
    id:            ID[Post],
    authorID:      ID[User],
    channelID:     ID[Channel],
    urlTitle:      Post.UrlTitle,
    title:         Post.Title,
    content:       Post.Content,
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Updated(
    id:            ID[Post],
    editorID:      ID[User],
    newUrlTitle:   Updatable[Post.UrlTitle],
    newTitle:      Updatable[Post.Title],
    newContent:    Updatable[Post.Content],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Deleted(
    id:            ID[Post],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restored(
    id:            ID[Post],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Upvoted(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Downvoted(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class VoteRevoked(
    id:            ID[Post],
    voterID:       ID[User],
    correlationID: CorrelationID
  ) extends PostEvent
}
