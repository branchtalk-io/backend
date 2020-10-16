package io.branchtalk.discussions.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{
  DiscussionCommandEvent,
  DiscussionEvent,
  SubscriptionCommandEvent,
  SubscriptionEvent
}
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.models.UUID
import io.scalaland.chimney.dsl._

final class SubscriptionProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, DiscussionCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  private implicit val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect {
        case DiscussionCommandEvent.ForSubscription(event) => event
      }
      .evalMap[F, (UUID, SubscriptionEvent)] {
        case event: SubscriptionCommandEvent.Subscribe   => toSubscribe(event).widen
        case event: SubscriptionCommandEvent.Unsubscribe => toUnsubscribe(event).widen
      }
      .map {
        case (key, value) => key -> DiscussionEvent.ForSubscription(value)
      }
      .handleErrorWith { error =>
        logger.error("Subscription event processing failed", error)
        Stream.empty
      }

  def toSubscribe(event: SubscriptionCommandEvent.Subscribe): F[(UUID, SubscriptionEvent.Subscribed)] =
    sql"""INSERT INTO subscriptions (
         |  subscriber_id,
         |  subscriptions_ids
         |)
         |VALUES (
         |  ${event.subscriberID},
         |  ${event.subscriptions}
         |)
         |ON CONFLICT (subscriber_id) DO
         |UPDATE
         |SET subscriptions_ids = array_distinct(subscriptions.subscriptions_ids || ${event.subscriptions})""".stripMargin.update.run
      .transact(transactor) >>
      (event.subscriberID.uuid -> event.transformInto[SubscriptionEvent.Subscribed]).pure[F]

  def toUnsubscribe(event: SubscriptionCommandEvent.Unsubscribe): F[(UUID, SubscriptionEvent.Unsubscribed)] = 
    sql"""UPDATE subscriptions
         |SET subscriptions_ids = array_diff(subscriptions.subscriptions_ids, ${event.subscriptions})
         |WHERE subscriber_id = ${event.subscriberID}""".stripMargin.update.run.transact(transactor) >>
      (event.subscriberID.uuid -> event.transformInto[SubscriptionEvent.Unsubscribed]).pure[F]
}
