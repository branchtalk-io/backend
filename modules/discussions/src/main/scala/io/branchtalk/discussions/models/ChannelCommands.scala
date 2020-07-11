package io.branchtalk.discussions.models

import cats.implicits._
import io.scalaland.catnip.Semi
import io.branchtalk.shared.models.{ FastEq, ID, OptionUpdatable, ShowPretty, Updatable }

trait ChannelCommands { self: Channel.type =>
  type Create = ChannelCommands.Create
  type Update = ChannelCommands.Update
  type Delete = ChannelCommands.Delete
}
object ChannelCommands {

  @Semi(FastEq, ShowPretty) final case class Create(
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  )

  @Semi(FastEq, ShowPretty) final case class Update(
    id:          ID[Channel],
    editorID:    ID[User],
    urlName:     Updatable[Channel.UrlName],
    name:        Updatable[Channel.Name],
    description: OptionUpdatable[Channel.Description]
  )

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Channel],
    editorID: ID[User]
  )
}
