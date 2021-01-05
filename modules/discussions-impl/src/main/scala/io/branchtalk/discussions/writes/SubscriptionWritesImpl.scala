package io.branchtalk.discussions.writes

import cats.effect.{ Sync, Timer }
import io.branchtalk.discussions.events.{ DiscussionsCommandEvent, SubscriptionCommandEvent }
import io.branchtalk.discussions.model.{ Subscription, User }
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model._
import io.scalaland.chimney.dsl._

final class SubscriptionWritesImpl[F[_]: Sync: Timer: MDC](
  producer:   EventBusProducer[F, DiscussionsCommandEvent],
  transactor: Transactor[F]
)(implicit
  uuidGenerator: UUIDGenerator
) extends Writes[F, User, DiscussionsCommandEvent](producer)
    with SubscriptionWrites[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT subscriber_id,
        |       subscriptions_ids
        |FROM subscriptions""".stripMargin

  override def subscribe(subscribe: Subscription.Subscribe): F[Subscription.Scheduled] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = subscribe.subscriberID
      now <- ModificationTime.now[F]
      command = subscribe
        .into[SubscriptionCommandEvent.Subscribe]
        .withFieldConst(_.modifiedAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForSubscription(command))
      subscription <- (commonSelect ++ fr"WHERE subscriber_id = ${id}")
        .query[Subscription]
        .option
        .map(_.getOrElse(Subscription(id, Set.empty)))
        .transact(transactor)
    } yield Subscription.Scheduled(subscription ++ subscribe.transformInto[Subscription].subscriptions)

  override def unsubscribe(unsubscribe: Subscription.Unsubscribe): F[Subscription.Scheduled] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = unsubscribe.subscriberID
      now <- ModificationTime.now[F]
      command = unsubscribe
        .into[SubscriptionCommandEvent.Unsubscribe]
        .withFieldConst(_.modifiedAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForSubscription(command))
      subscription <- (commonSelect ++ fr"WHERE subscriber_id = ${id}")
        .query[Subscription]
        .option
        .map(_.getOrElse(Subscription(id, Set.empty)))
        .transact(transactor)
    } yield Subscription.Scheduled(subscription -- unsubscribe.transformInto[Subscription].subscriptions)
}
