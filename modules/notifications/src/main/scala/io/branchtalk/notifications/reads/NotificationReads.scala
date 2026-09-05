package io.branchtalk.notifications.reads

import io.branchtalk.notifications.model.{ Notification, User }
import io.branchtalk.shared.model.{ ID, Paginated }

trait NotificationReads[F[_]] {

  def paginate(
    recipientID: ID[User],
    unreadOnly:  Boolean,
    sorting:     Notification.Sorting,
    offset:      Paginated.Offset,
    limit:       Paginated.Limit
  ): F[Paginated[Notification]]

  def exists(id: ID[Notification]): F[Boolean]

  def getById(id: ID[Notification]): F[Option[Notification]]

  def requireById(id: ID[Notification]): F[Notification]
}
