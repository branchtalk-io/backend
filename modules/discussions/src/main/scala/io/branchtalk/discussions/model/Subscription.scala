package io.branchtalk.discussions.model

import io.branchtalk.shared.model.{ FastEq, ID, ShowPretty }

import scala.annotation.targetName

final case class Subscription(
  subscriberID:  ID[User],
  subscriptions: Set[ID[Channel]]
) derives FastEq,
      ShowPretty {

  @targetName("addAll")
  def ++(subscriptions: Set[ID[Channel]]): Subscription =
    Subscription(subscriberID = subscriberID, subscriptions = this.subscriptions ++ subscriptions)

  @targetName("removeAll")
  def --(subscriptions: Set[ID[Channel]]): Subscription =
    Subscription(subscriberID = subscriberID, subscriptions = this.subscriptions -- subscriptions)
}
object Subscription {

  final case class Scheduled(
    subscription: Subscription
  ) derives FastEq,
        ShowPretty

  final case class Subscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]]
  ) derives FastEq,
        ShowPretty

  final case class Unsubscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]]
  ) derives FastEq,
        ShowPretty
}
