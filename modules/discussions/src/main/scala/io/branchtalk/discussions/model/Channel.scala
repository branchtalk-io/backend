package io.branchtalk.discussions.model

import io.scalaland.catnip.Semi
import io.branchtalk.shared.model.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty }

@Semi(FastEq, ShowPretty) final case class Channel(
  id:   ID[Channel],
  data: Channel.Data
)
object Channel extends ChannelProperties with ChannelCommands {

  @Semi(FastEq, ShowPretty) final case class Data(
    urlName:        Channel.UrlName,
    name:           Channel.Name,
    description:    Option[Channel.Description],
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  )
}
