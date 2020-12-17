package io.branchtalk.users.model

import io.branchtalk.shared.model.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

trait BanCommands {
  type Order = BanCommands.Order
  type Lift  = BanCommands.Lift
  val Order = BanCommands.Order
  val Lift  = BanCommands.Lift
}
object BanCommands {

  @Semi(FastEq, ShowPretty) final case class Order(
    bannedUserID: ID[User],
    reason:       Ban.Reason,
    scope:        Ban.Scope,
    moderatorID:  Option[ID[User]]
  )

  @Semi(FastEq, ShowPretty) final case class Lift(
    bannedUserID: ID[User],
    scope:        Ban.Scope,
    moderatorID:  Option[ID[User]]
  )
}
