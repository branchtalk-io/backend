package io.branchtalk.discussions.reads

import cats.effect.Sync
import io.branchtalk.discussions.dao.Comment
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models

final class CommentReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends CommentReads[F] {

  private implicit val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""
      SELECT id,
             author_id,
             post_id,
             content,
             reply_to,
             nesting_level,
             created_at,
             last_modified_at
      FROM comments
    """

  override def exists(id: models.ID[Comment]): F[Boolean] =
    sql"SELECT EXISTS(SELECT 1 FROM comments WHERE id = ${id})".query[Boolean].unique.transact(transactor)

  override def getById(id: models.ID[Comment]): F[Option[Comment]] =
    (commonSelect ++ fr"WHERE id = ${id}").query[Comment].option.transact(transactor)

  override def requireById(id: models.ID[Comment]): F[Comment] =
    (commonSelect ++ fr"WHERE id = ${id}").query[Comment].failNotFound("Comment", id).transact(transactor)
}
