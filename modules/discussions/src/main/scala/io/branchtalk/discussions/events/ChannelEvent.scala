package io.branchtalk.discussions.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait ChannelEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object ChannelEvent {

  final case class Created(
    id:            ID[Channel],
    authorID:      ID[User],
    urlName:       Channel.UrlName,
    name:          Channel.Name,
    description:   Option[Channel.Description],
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends ChannelEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Updated(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description],
    modifiedAt:     ModificationTime,
    correlationID:  CorrelationID
  ) extends ChannelEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Deleted(
    id:            ID[Channel],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends ChannelEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Restored(
    id:            ID[Channel],
    editorID:      ID[User],
    correlationID: CorrelationID
  ) extends ChannelEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
