package io.branchtalk.users.model

import io.branchtalk.ADT
import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) sealed trait Permission extends ADT
object Permission extends PermissionCommands {

  final case class EditProfile(userID:        ID[User]) extends Permission
  final case class ModerateChannel(channelID: ID[Channel]) extends Permission
  case object ModerateUsers extends Permission
}
