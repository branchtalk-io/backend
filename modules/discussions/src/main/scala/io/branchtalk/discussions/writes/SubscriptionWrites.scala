package io.branchtalk.discussions.writes

import io.branchtalk.discussions.model.Subscription

trait SubscriptionWrites[F[_]] {

  def subscribe(subscribe:     Subscription.Subscribe):   F[Subscription.Scheduled]
  def unsubscribe(unsubscribe: Subscription.Unsubscribe): F[Subscription.Scheduled]
}
