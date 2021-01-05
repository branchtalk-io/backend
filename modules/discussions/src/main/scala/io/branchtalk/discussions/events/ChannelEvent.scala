package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait ChannelEvent extends ADT
object ChannelEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Created(
    id:            ID[Channel],
    authorID:      ID[User],
    urlName:       Channel.UrlName,
    name:          Channel.Name,
    description:   Option[Channel.Description],
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends ChannelEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Updated(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description],
    modifiedAt:     ModificationTime,
    correlationID:  CorrelationID
  ) extends ChannelEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Deleted(
    id:            ID[Channel],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends ChannelEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restored(
    id:            ID[Channel],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends ChannelEvent
}
