package io.branchtalk.users.model

import io.branchtalk.ADT
import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

trait SettingCommands {
  type Update = SettingCommands.Update
  val Update = SettingCommands.Update
}
object SettingCommands {

  @Semi(FastEq, ShowPretty) sealed trait Update extends ADT
  object Update {
    final case class AddSubscription(channelID:    ID[Channel]) extends Update
    final case class RemoveSubscription(channelID: ID[Channel]) extends Update
  }
}
