package io.branchtalk.discussions.reads

import io.branchtalk.discussions.model.{ Subscription, User }
import io.branchtalk.shared.models.ID

trait SubscriptionReads[F[_]] {

  def requireForUser(userID: ID[User]): F[Subscription]
}
