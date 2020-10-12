package io.branchtalk.discussions.reads

import cats.effect.Sync
import io.branchtalk.discussions.model.{ Subscription, User }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models._

final class SubscriptionReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends SubscriptionReads[F] {

  private implicit val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT subscriber_id,
        |       subscriptions_ids
        |FROM subscriptions""".stripMargin

  override def requireForUser(userID: ID[User]): F[Subscription] =
    (commonSelect ++ fr"WHERE subscriber_id = ${userID}")
      .query[Subscription]
      .option
      .map(_.getOrElse(Subscription(userID, Set.empty)))
      .transact(transactor)
}
