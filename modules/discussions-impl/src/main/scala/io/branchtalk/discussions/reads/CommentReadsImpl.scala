package io.branchtalk.discussions.reads

import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.discussions.model.{ Comment, Post }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model.{ ID, Paginated }

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
        |       replies_nr,
        |       upvotes_nr,
        |       downvotes_nr,
        |       total_score,
        |       controversial_score
        |FROM comments""".stripMargin

  private val orderBy: Comment.Sorting => Fragment = {
    case Comment.Sorting.Newest        => fr"ORDER BY created_at DESC"
    case Comment.Sorting.Hottest       => fr"ORDER by total_score DESC"
    case Comment.Sorting.Controversial => fr"ORDER by controversial_score DESC"
  }

  private def idExists(id: ID[Comment]): Fragment = fr"id = ${id} AND deleted = FALSE"

  private def idDeleted(id: ID[Comment]): Fragment = fr"id = ${id} AND deleted = TRUE"

  override def paginate(
    post:      ID[Post],
    repliesTo: Option[ID[Comment]],
    sortBy:    Comment.Sorting,
    offset:    Long Refined NonNegative,
    limit:     Int Refined Positive
  ): F[Paginated[Comment]] =
    (commonSelect ++ Fragments.whereAndOpt(fr"post_id = $post".some,
                                           repliesTo.map(parent => fr"reply_to = $parent"),
                                           fr"deleted = FALSE".some
    ) ++ orderBy(sortBy)).paginate[Comment](offset, limit).transact(transactor)

  override def exists(id: ID[Comment]): F[Boolean] =
    (fr"SELECT 1 FROM comments WHERE" ++ idExists(id)).exists.transact(transactor)

  override def deleted(id: ID[Comment]): F[Boolean] =
    (fr"SELECT 1 FROM comments WHERE" ++ idDeleted(id)).exists.transact(transactor)

  override def getById(id: ID[Comment], isDeleted: Boolean = false): F[Option[Comment]] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[Comment]
      .option
      .transact(transactor)

  override def requireById(id: ID[Comment], isDeleted: Boolean = false): F[Comment] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[Comment]
      .failNotFound("Comment", id)
      .transact(transactor)
}
