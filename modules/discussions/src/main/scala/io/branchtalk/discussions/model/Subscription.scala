package io.branchtalk.discussions.model

import io.branchtalk.shared.model.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) final case class Subscription(
  subscriberID:  ID[User],
  subscriptions: Set[ID[Channel]]
) {

  def ++(subscriptions: Set[ID[Channel]]): Subscription = // scalastyle:ignore method.name
    Subscription(subscriberID = subscriberID, subscriptions = this.subscriptions ++ subscriptions)

  def --(subscriptions: Set[ID[Channel]]): Subscription = // scalastyle:ignore method.name
    Subscription(subscriberID = subscriberID, subscriptions = this.subscriptions -- subscriptions)
}
object Subscription extends SubscriptionCommands {

  @Semi(FastEq, ShowPretty) final case class Scheduled(subscription: Subscription)
}
