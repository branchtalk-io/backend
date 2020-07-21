package io.branchtalk.discussions.models

import io.scalaland.catnip.Semi
import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty }

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
