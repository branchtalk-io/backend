package io.branchtalk.discussions.model

import io.branchtalk.shared.model.{ FastEq, ID, ShowPretty }

trait SubscriptionCommands {
  type Subscribe   = SubscriptionCommands.Subscribe
  type Unsubscribe = SubscriptionCommands.Unsubscribe
  val Subscribe   = SubscriptionCommands.Subscribe
  val Unsubscribe = SubscriptionCommands.Unsubscribe
}
object SubscriptionCommands {

  final case class Subscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]]
  ) derives FastEq, ShowPretty

  final case class Unsubscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]]
  ) derives FastEq, ShowPretty
}
