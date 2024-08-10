package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait ChannelCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object ChannelCommandEvent {

  final case class Create(
    id:            ID[Channel],
    authorID:      ID[User],
    urlName:       Channel.UrlName,
    name:          Channel.Name,
    description:   Option[Channel.Description],
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends ChannelCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Update(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description],
    modifiedAt:     ModificationTime,
    correlationID:  CorrelationID
  ) extends ChannelCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Delete(
    id:            ID[Channel],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends ChannelCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Restore(
    id:            ID[Channel],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends ChannelCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
