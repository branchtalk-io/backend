package io.branchtalk.discussions.writes

import cats.effect.{ Sync, Timer }
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, SubscriptionCommandEvent }
import io.branchtalk.discussions.model.{ Subscription, User }
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models._
import io.scalaland.chimney.dsl._

final class SubscriptionWritesImpl[F[_]: Sync: Timer](
  producer:   EventBusProducer[F, DiscussionCommandEvent],
  transactor: Transactor[F]
) extends Writes[F, User, DiscussionCommandEvent](producer)
    with SubscriptionWrites[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT subscriber_id,
        |       subscriptions_ids
        |FROM subscriptions""".stripMargin

  override def subscribe(subscribe: Subscription.Subscribe): F[Subscription.Scheduled] =
    for {
      id <- subscribe.subscriberID.pure[F]
      now <- ModificationTime.now[F]
      command = subscribe.into[SubscriptionCommandEvent.Subscribe].withFieldConst(_.modifiedAt, now).transform
      _ <- postEvent(id, DiscussionCommandEvent.ForSubscription(command))
      subscription <- (commonSelect ++ fr"WHERE subscriber_id = ${id}")
        .query[Subscription]
        .option
        .map(_.getOrElse(Subscription(id, Set.empty)))
        .transact(transactor)
    } yield Subscription.Scheduled(subscription ++ subscribe.transformInto[Subscription].subscriptions)

  override def unsubscribe(unsubscribe: Subscription.Unsubscribe): F[Subscription.Scheduled] =
    for {
      id <- unsubscribe.subscriberID.pure[F]
      now <- ModificationTime.now[F]
      command = unsubscribe.into[SubscriptionCommandEvent.Unsubscribe].withFieldConst(_.modifiedAt, now).transform
      _ <- postEvent(id, DiscussionCommandEvent.ForSubscription(command))
      subscription <- (commonSelect ++ fr"WHERE subscriber_id = ${id}")
        .query[Subscription]
        .option
        .map(_.getOrElse(Subscription(id, Set.empty)))
        .transact(transactor)
    } yield Subscription.Scheduled(subscription -- unsubscribe.transformInto[Subscription].subscriptions)
}
