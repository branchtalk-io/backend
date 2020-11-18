package io.branchtalk.discussions.reads

import cats.effect.Sync
import io.branchtalk.discussions.model.Comment
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model

final class CommentReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends CommentReads[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       author_id,
        |       channel_id,
        |       post_id,
        |       content,
        |       reply_to,
        |       nesting_level,
        |       created_at,
        |       last_modified_at,
        |       replies_nr
        |FROM comments""".stripMargin

  private def idExists(id: model.ID[Comment]): Fragment = fr"id = ${id} AND deleted = FALSE"

  private def idDeleted(id: model.ID[Comment]): Fragment = fr"id = ${id} AND deleted = TRUE"

  override def exists(id: model.ID[Comment]): F[Boolean] =
    (fr"SELECT 1 FROM comments WHERE" ++ idExists(id)).exists.transact(transactor)

  override def deleted(id: model.ID[Comment]): F[Boolean] =
    (fr"SELECT 1 FROM comments WHERE" ++ idDeleted(id)).exists.transact(transactor)

  override def getById(id: model.ID[Comment], isDeleted: Boolean = false): F[Option[Comment]] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[Comment]
      .option
      .transact(transactor)

  override def requireById(id: model.ID[Comment], isDeleted: Boolean = false): F[Comment] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[Comment]
      .failNotFound("Comment", id)
      .transact(transactor)
}
