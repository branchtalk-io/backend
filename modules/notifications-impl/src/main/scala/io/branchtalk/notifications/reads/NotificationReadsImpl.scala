package io.branchtalk.notifications.reads

import cats.effect.Sync
import io.branchtalk.notifications.infrastructure.DoobieExtensions.{ *, given }
import io.branchtalk.notifications.model.{ Notification, User }
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.{ ID, Paginated }

final class NotificationReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends NotificationReads[F] {

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       recipient_id,
        |       kind,
        |       source_post_id,
        |       source_comment_id,
        |       source_user_id,
        |       message,
        |       created_at,
        |       read_at
        |FROM notifications""".stripMargin

  override def paginate(
    recipientID: ID[User],
    unreadOnly:  Boolean,
    sorting:     Notification.Sorting,
    offset:      Paginated.Offset,
    limit:       Paginated.Limit
  ): F[Paginated[Notification]] = {
    val unreadFilter = if (unreadOnly) fr"read_at IS NULL".some else none[Fragment]
    (commonSelect ++ Fragments.whereAndOpt(
      fr"recipient_id = $recipientID".some,
      unreadFilter
    ) ++ fr"ORDER BY created_at DESC")
      .paginate[Notification](offset, limit, show"Paginate Notifications from $offset taking $limit")
      .transact(transactor)
  }

  override def exists(id: ID[Notification]): F[Boolean] =
    fr"SELECT 1 FROM notifications WHERE id = $id".exists(show"Notifications ID=$id exists").transact(transactor)

  override def getById(id: ID[Notification]): F[Option[Notification]] =
    (commonSelect ++ fr"WHERE id = $id")
      .queryWithLabel[Notification](show"Get Notification by ID=$id")
      .option
      .transact(transactor)

  override def requireById(id: ID[Notification]): F[Notification] =
    (commonSelect ++ fr"WHERE id = $id")
      .queryWithLabel[Notification](show"Require Notification by ID=$id")
      .failNotFound("Notification", id)
      .transact(transactor)
}
