package io.branchtalk.notifications.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.notifications.events.{ NotificationCommandEvent, NotificationEvent }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID

final class NotificationCommandHandler[F[_]: Sync]
    extends Projector[F, NotificationCommandEvent, (UUID, NotificationEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, NotificationCommandEvent]): Stream[F, (UUID, NotificationEvent)] =
    in.evalMap[F, (UUID, NotificationEvent)] {
      case command: NotificationCommandEvent.Create      => toCreate(command).widen
      case command: NotificationCommandEvent.MarkRead    => toMarkRead(command).widen
      case command: NotificationCommandEvent.MarkAllRead => toMarkAllRead(command).widen
    }.handleErrorWith { error =>
      logger.error("Notification command processing failed", error)
      Stream.empty
    }

  def toCreate(command: NotificationCommandEvent.Create): F[(UUID, NotificationEvent.Created)] =
    (command.id.unwrap -> NotificationEvent.Created(
      id = command.id,
      recipientID = command.recipientID,
      kind = command.kind,
      sourcePostID = command.sourcePostID,
      sourceCommentID = command.sourceCommentID,
      sourceUserID = command.sourceUserID,
      message = command.message,
      createdAt = command.createdAt,
      correlationID = command.correlationID
    )).pure[F]

  def toMarkRead(command: NotificationCommandEvent.MarkRead): F[(UUID, NotificationEvent.Read)] =
    (command.id.unwrap -> NotificationEvent.Read(
      id = command.id,
      userID = command.userID,
      readAt = command.readAt,
      correlationID = command.correlationID
    )).pure[F]

  def toMarkAllRead(command: NotificationCommandEvent.MarkAllRead): F[(UUID, NotificationEvent.AllRead)] =
    (command.recipientID.unwrap -> NotificationEvent.AllRead(
      recipientID = command.recipientID,
      readAt = command.readAt,
      correlationID = command.correlationID
    )).pure[F]
}
