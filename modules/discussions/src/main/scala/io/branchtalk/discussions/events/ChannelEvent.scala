package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport.*

sealed trait ChannelEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object ChannelEvent {

  final case class Created(
    id:            ID[Channel],
    authorID:      ID[User],
    urlName:       Channel.UrlName,
    name:          Channel.Name,
    description:   Option[Channel.Description],
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends ChannelEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor 

  final case class Updated(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description],
    modifiedAt:     ModificationTime,
    correlationID:  CorrelationID
  ) extends ChannelEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Deleted(
    id:            ID[Channel],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends ChannelEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Restored(
    id:            ID[Channel],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends ChannelEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
