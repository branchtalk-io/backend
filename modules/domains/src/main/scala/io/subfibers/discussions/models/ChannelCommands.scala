package io.subfibers.discussions.models

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models.ID
import io.subfibers.users.models.User

trait ChannelCommands { self: Channel.type =>
  type Create = ChannelCommands.Create
  type Update = ChannelCommands.Update
  type Delete = ChannelCommands.Delete
}
object ChannelCommands {

  @Semi(Eq, ShowPretty) final case class Create(
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  )

  @Semi(Eq, ShowPretty) final case class Update(
    id:          ID[Channel],
    editorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  )

  @Semi(Eq, ShowPretty) final case class Delete(
    id:       ID[Channel],
    editorID: ID[User]
  )
}
