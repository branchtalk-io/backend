package io.branchtalk.discussions.model

import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

trait SubscriptionCommands {
  type Subscribe   = SubscriptionCommands.Subscribe
  type Unsubscribe = SubscriptionCommands.Unsubscribe
  val Subscribe   = SubscriptionCommands.Subscribe
  val Unsubscribe = SubscriptionCommands.Unsubscribe
}
object SubscriptionCommands {

  @Semi(FastEq, ShowPretty) final case class Subscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]]
  )

  @Semi(FastEq, ShowPretty) final case class Unsubscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]]
  )
}
