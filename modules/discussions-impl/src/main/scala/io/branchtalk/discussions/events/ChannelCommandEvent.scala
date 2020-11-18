package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait ChannelCommandEvent extends ADT
object ChannelCommandEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Create(
    id:          ID[Channel],
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description],
    createdAt:   CreationTime
  ) extends ChannelCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Update(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description],
    modifiedAt:     ModificationTime
  ) extends ChannelCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Delete(
    id:       ID[Channel],
    editorID: ID[User]
  ) extends ChannelCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restore(
    id:       ID[Channel],
    editorID: ID[User]
  ) extends ChannelCommandEvent
}
