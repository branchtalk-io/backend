package io.branchtalk.users.model

import io.branchtalk.shared.model.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

trait BanCommands {
  type InChannel     = BanCommands.BanInChannel
  type LiftInChannel = BanCommands.LiftInChannel
  type Globally      = BanCommands.BanGlobally
  type LiftGlobally  = BanCommands.LiftGlobally
  val InChannel     = BanCommands.BanInChannel
  val LiftInChannel = BanCommands.LiftInChannel
  val Globally      = BanCommands.BanGlobally
  val LiftGlobally  = BanCommands.LiftGlobally
}
object BanCommands {

  @Semi(FastEq, ShowPretty) final case class BanInChannel(
    bannedUserID: ID[User],
    reason:       Ban.Reason,
    scope:        Ban.Scope
  )

  @Semi(FastEq, ShowPretty) final case class LiftInChannel(
    bannedUserID: ID[User],
    scope:        Ban.Scope
  )

  @Semi(FastEq, ShowPretty) final case class BanGlobally(
    bannedUserID: ID[User],
    reason:       Ban.Reason,
    scope:        Ban.Scope
  )

  @Semi(FastEq, ShowPretty) final case class LiftGlobally(
    bannedUserID: ID[User],
    scope:        Ban.Scope
  )
}
