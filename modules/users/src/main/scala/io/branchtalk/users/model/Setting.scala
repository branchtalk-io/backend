package io.branchtalk.users.model

import io.branchtalk.ADT
import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) sealed trait Setting extends ADT
object Setting extends SettingCommands {

  final case class Subscriptions(list: List[ID[Channel]]) extends Setting
}
