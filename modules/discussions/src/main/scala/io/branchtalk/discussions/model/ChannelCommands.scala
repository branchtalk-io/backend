package io.branchtalk.discussions.model

import io.scalaland.catnip.Semi
import io.branchtalk.shared.models.{ FastEq, ID, OptionUpdatable, ShowPretty, Updatable }

trait ChannelCommands { self: Channel.type =>
  type Create  = ChannelCommands.Create
  type Update  = ChannelCommands.Update
  type Delete  = ChannelCommands.Delete
  type Restore = ChannelCommands.Restore
  val Create  = ChannelCommands.Create
  val Update  = ChannelCommands.Update
  val Delete  = ChannelCommands.Delete
  val Restore = ChannelCommands.Restore
}
object ChannelCommands {

  @Semi(FastEq, ShowPretty) final case class Create(
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  )

  @Semi(FastEq, ShowPretty) final case class Update(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description]
  )

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Channel],
    editorID: ID[User]
  )

  @Semi(FastEq, ShowPretty) final case class Restore(
    id:       ID[Channel],
    editorID: ID[User]
  )
}
