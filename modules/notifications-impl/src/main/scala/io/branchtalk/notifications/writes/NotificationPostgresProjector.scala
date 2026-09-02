package io.branchtalk.notifications.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import org.typelevel.doobie.Transactor
import fs2.Stream
import io.branchtalk.notifications.events.NotificationEvent
import io.branchtalk.notifications.model.Notification
import io.branchtalk.notifications.infrastructure.DoobieExtensions.{ *, given }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID

final class NotificationPostgresProjector[F[_]: Sync: MDC](
  transactor:        Transactor[F],
  notificationTopic: NotificationTopic[F]
) extends Projector[F, NotificationEvent, (UUID, NotificationEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, NotificationEvent]): Stream[F, (UUID, NotificationEvent)] =
    in.evalMap[F, (UUID, NotificationEvent)] {
      case event: NotificationEvent.Created => toCreate(event).widen
      case event: NotificationEvent.Read    => toMarkRead(event).widen
      case event: NotificationEvent.AllRead => toMarkAllRead(event).widen
    }.handleErrorWith { error =>
      logger.error("Notification event processing failed", error)
      Stream.empty
    }

  def toCreate(event: NotificationEvent.Created): F[(UUID, NotificationEvent.Created)] =
    withCorrelationID(event.correlationID) {
      sql"""INSERT INTO notifications (
           |  id,
           |  recipient_id,
           |  kind,
           |  source_post_id,
           |  source_comment_id,
           |  source_user_id,
           |  message,
           |  created_at
           |)
           |VALUES (
           |  ${event.id},
           |  ${event.recipientID},
           |  ${event.kind},
           |  ${event.sourcePostID},
           |  ${event.sourceCommentID},
           |  ${event.sourceUserID},
           |  ${event.message},
           |  ${event.createdAt}
           |)
           |ON CONFLICT (id) DO NOTHING""".stripMargin
        .updateWithLabel(show"Create Notification ID=${event.id}")
        .run
        .as(event.id.unwrap -> event)
        .transact(transactor)
        .flatTap(_ => notificationTopic.publish(eventToNotification(event)))
    }

  private def eventToNotification(event: NotificationEvent.Created): Notification =
    Notification(
      id = event.id,
      data = Notification.Data(
        recipientID = event.recipientID,
        kind = event.kind,
        sourcePostID = event.sourcePostID,
        sourceCommentID = event.sourceCommentID,
        sourceUserID = event.sourceUserID,
        message = event.message,
        createdAt = event.createdAt,
        readAt = None
      )
    )

  def toMarkRead(event: NotificationEvent.Read): F[(UUID, NotificationEvent.Read)] =
    withCorrelationID(event.correlationID) {
      sql"""UPDATE notifications
           |SET read_at = ${event.readAt}
           |WHERE id = ${event.id}
           |  AND recipient_id = ${event.userID}
           |  AND read_at IS NULL""".stripMargin
        .updateWithLabel(show"Mark read Notification ID=${event.id}")
        .run
        .as(event.id.unwrap -> event)
        .transact(transactor)
    }

  def toMarkAllRead(event: NotificationEvent.AllRead): F[(UUID, NotificationEvent.AllRead)] =
    withCorrelationID(event.correlationID) {
      sql"""UPDATE notifications
           |SET read_at = ${event.readAt}
           |WHERE recipient_id = ${event.recipientID}
           |  AND read_at IS NULL""".stripMargin
        .updateWithLabel(show"Mark all read for recipient=${event.recipientID}")
        .run
        .as(event.recipientID.unwrap -> event)
        .transact(transactor)
    }
}
