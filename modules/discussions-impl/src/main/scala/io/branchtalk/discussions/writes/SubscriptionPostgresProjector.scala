package io.branchtalk.discussions.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{ DiscussionEvent, SubscriptionEvent }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID

final class SubscriptionPostgresProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, DiscussionEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionEvent.ForSubscription(event) =>
      event
    }.evalMap[F, (UUID, SubscriptionEvent)] {
      case event: SubscriptionEvent.Subscribed   => toSubscribe(event).widen
      case event: SubscriptionEvent.Unsubscribed => toUnsubscribe(event).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForSubscription(value)
    }.handleErrorWith { error =>
      logger.error("Subscription event processing failed", error)
      Stream.empty
    }

  def toSubscribe(event: SubscriptionEvent.Subscribed): F[(UUID, SubscriptionEvent.Subscribed)] =
    withCorrelationID(event.correlationID) {
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
           |SET subscriptions_ids = array_distinct(subscriptions.subscriptions_ids || ${event.subscriptions})""" //
        .stripMargin.update.run.as(event.subscriberID.uuid -> event).transact(transactor)
    }

  def toUnsubscribe(event: SubscriptionEvent.Unsubscribed): F[(UUID, SubscriptionEvent.Unsubscribed)] =
    withCorrelationID(event.correlationID) {
      sql"""UPDATE subscriptions
           |SET subscriptions_ids = array_diff(subscriptions.subscriptions_ids, ${event.subscriptions})
           |WHERE subscriber_id = ${event.subscriberID}""".stripMargin.update.run
        .as(event.subscriberID.uuid -> event)
        .transact(transactor)
    }
}
