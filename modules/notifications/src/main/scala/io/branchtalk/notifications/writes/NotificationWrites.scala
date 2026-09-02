package io.branchtalk.notifications.writes

import io.branchtalk.notifications.model.Notification

trait NotificationWrites[F[_]] {

  def markRead(command:    Notification.MarkRead):    F[Unit]
  def markAllRead(command: Notification.MarkAllRead): F[Unit]
}
