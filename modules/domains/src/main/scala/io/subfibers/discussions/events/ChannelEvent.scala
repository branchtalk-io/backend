package io.subfibers.discussions.events

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.ADT
import io.subfibers.discussions.models.Channel
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime, OptionUpdatable, Updatable }
import io.subfibers.users.models.User

@Semi(Eq, ShowPretty) sealed trait ChannelEvent extends ADT
object ChannelEvent {

  @Semi(Eq, ShowPretty) final case class Created(
    id:          ID[Channel],
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description],
    createdAt:   CreationTime
  ) extends ChannelEvent

  @Semi(Eq, ShowPretty) final case class Updated(
    id:          ID[Channel],
    editorID:    ID[User],
    urlName:     Updatable[Channel.UrlName],
    name:        Updatable[Channel.Name],
    description: OptionUpdatable[Channel.Description],
    modifiedAt:  ModificationTime
  ) extends ChannelEvent

  @Semi(Eq, ShowPretty) final case class Deleted(
    id:       ID[Channel],
    editorID: ID[User]
  ) extends ChannelEvent
}
