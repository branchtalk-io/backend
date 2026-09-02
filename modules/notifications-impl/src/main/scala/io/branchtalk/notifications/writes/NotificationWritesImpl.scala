package io.branchtalk.notifications.writes

import cats.effect.Sync
import io.branchtalk.logging.*
import io.branchtalk.notifications.events.NotificationCommandEvent
import io.branchtalk.notifications.model.{ Notification, User }
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.*

final class NotificationWritesImpl[F[_]: Sync: MDC](
  producer:   KafkaEventBus.Producer[F, NotificationCommandEvent],
  transactor: Transactor[F]
)(using UUID.Generator)
    extends NotificationWrites[F] {

  override def markRead(command: Notification.MarkRead): F[Unit] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      now <- ModificationTime.now[F]
      event = NotificationCommandEvent.MarkRead(
        id = command.id,
        userID = command.userID,
        readAt = now,
        correlationID = correlationID
      )
      _ <- postEvent(command.id, event)
    } yield ()

  override def markAllRead(command: Notification.MarkAllRead): F[Unit] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      now <- ModificationTime.now[F]
      event = NotificationCommandEvent.MarkAllRead(
        recipientID = command.recipientID,
        readAt = now,
        correlationID = correlationID
      )
      // Use a synthetic UUID for the key since MarkAllRead is not tied to a single notification
      syntheticID <- UUID.create[F]
      _ <- postEvent(ID[Notification](syntheticID), event)
    } yield ()

  private def postEvent(id: ID[Notification], event: NotificationCommandEvent): F[Unit] =
    producer(fs2.Stream[F, (UUID, NotificationCommandEvent)](id.unwrap -> event)).compile.drain
}
