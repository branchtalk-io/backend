package io.subfibers.discussions.models

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }

@Semi(Eq, ShowPretty) final case class Channel(
  id:   ID[Channel],
  data: Channel.Data
)
object Channel extends ChannelProperties with ChannelCommands {

  @Semi(Eq, ShowPretty) final case class Data(
    urlName:        Channel.UrlName,
    name:           Channel.Name,
    description:    Option[Channel.Description],
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  )
}
