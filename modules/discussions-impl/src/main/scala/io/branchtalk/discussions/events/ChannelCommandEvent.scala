package io.branchtalk.discussions.events

import com.sksamuel.avro4s.SchemaFor
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.models.{ Channel, User }
import io.branchtalk.shared.infrastructure.AvroSupport._
import io.branchtalk.shared.models._

@Semi(FastEq, ShowPretty, SchemaFor) sealed trait ChannelCommandEvent extends ADT
object ChannelCommandEvent {

  @Semi(FastEq, ShowPretty) final case class Create(
    id:          ID[Channel],
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description],
    createdAt:   CreationTime
  ) extends ChannelCommandEvent

  @Semi(FastEq, ShowPretty) final case class Update(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description],
    modifiedAt:     ModificationTime
  ) extends ChannelCommandEvent

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Channel],
    editorID: ID[User]
  ) extends ChannelCommandEvent

  @Semi(FastEq, ShowPretty) final case class Restore(
    id:       ID[Channel],
    editorID: ID[User]
  ) extends ChannelCommandEvent
}
