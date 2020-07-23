package io.branchtalk.discussions.events

import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.shared.models._

@Semi(FastEq, ShowPretty) sealed trait ChannelEvent extends ADT
object ChannelEvent {

  @Semi(FastEq, ShowPretty) final case class Created(
    id:          ID[Channel],
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description],
    createdAt:   CreationTime
  ) extends ChannelEvent

  @Semi(FastEq, ShowPretty) final case class Updated(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description],
    modifiedAt:     ModificationTime
  ) extends ChannelEvent

  @Semi(FastEq, ShowPretty) final case class Deleted(
    id:       ID[Channel],
    editorID: ID[User]
  ) extends ChannelEvent

  @Semi(FastEq, ShowPretty) final case class Restored(
    id:       ID[Channel],
    editorID: ID[User]
  ) extends ChannelEvent
}
