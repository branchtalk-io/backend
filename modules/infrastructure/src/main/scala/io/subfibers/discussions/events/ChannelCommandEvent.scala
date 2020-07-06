package io.subfibers.discussions.events

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.ADT
import io.subfibers.discussions.models.Channel
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.models.User

@Semi(Eq, ShowPretty) sealed trait ChannelCommandEvent extends ADT
object ChannelCommandEvent {

  @Semi(Eq, ShowPretty) final case class Create(
    id:          ID[Channel],
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description],
    createdAt:   CreationTime
  ) extends ChannelCommandEvent

  @Semi(Eq, ShowPretty) final case class Update(
    id:             ID[Channel],
    editorID:       ID[User],
    urlName:        Channel.UrlName,
    name:           Channel.Name,
    description:    Option[Channel.Description],
    lastModifiedAt: ModificationTime
  ) extends ChannelCommandEvent

  @Semi(Eq, ShowPretty) final case class Delete(
    id:       ID[Channel],
    editorID: ID[User]
  ) extends ChannelCommandEvent
}
