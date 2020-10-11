package io.branchtalk.api

import io.branchtalk.ADT
import io.scalaland.catnip.Semi

@Semi(JsCodec) sealed trait Permission extends ADT
object Permission {

  final case class EditProfile(userID:        UserID) extends Permission
  final case class ModerateChannel(channelID: ChannelID) extends Permission
  case object ModerateUsers extends Permission
}
