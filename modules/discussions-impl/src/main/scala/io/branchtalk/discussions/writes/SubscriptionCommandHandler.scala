package io.branchtalk.discussions.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{
  DiscussionEvent,
  DiscussionsCommandEvent,
  SubscriptionCommandEvent,
  SubscriptionEvent
}
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.scalaland.chimney.dsl._

final class SubscriptionCommandHandler[F[_]: Sync]
    extends Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, DiscussionsCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionsCommandEvent.ForSubscription(command) =>
      command
    }.evalMap[F, (UUID, SubscriptionEvent)] {
      case command: SubscriptionCommandEvent.Subscribe   => toSubscribe(command).widen
      case command: SubscriptionCommandEvent.Unsubscribe => toUnsubscribe(command).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForSubscription(value)
    }.handleErrorWith { error =>
      logger.error("Subscription command processing failed", error)
      Stream.empty
    }

  def toSubscribe(command: SubscriptionCommandEvent.Subscribe): F[(UUID, SubscriptionEvent.Subscribed)] =
    (command.subscriberID.uuid -> command.transformInto[SubscriptionEvent.Subscribed]).pure[F]

  def toUnsubscribe(command: SubscriptionCommandEvent.Unsubscribe): F[(UUID, SubscriptionEvent.Unsubscribed)] =
    (command.subscriberID.uuid -> command.transformInto[SubscriptionEvent.Unsubscribed]).pure[F]
}
