package io.branchtalk.discussions.reads

import cats.data.NonEmptySet
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.discussions.infrastructure.DoobieExtensions._
import io.branchtalk.discussions.model.{ Channel, Post, PostDao }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model.{ ID, Paginated }

final class PostReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends PostReads[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       author_id,
        |       channel_id,
        |       url_title,
        |       title,
        |       content_type,
        |       content_raw,
        |       created_at,
        |       last_modified_at,
        |       comments_nr,
        |       upvotes,
        |       downvotes,
        |       total_score,
        |       controversial_score
        |FROM posts""".stripMargin

  private val orderBy: Post.Sorting => Fragment = {
    case Post.Sorting.Newest             => fr"ORDER BY created_at DESC"
    case Post.Sorting.TotalScore         => fr"ORDER by total_score DESC"
    case Post.Sorting.ControversialScore => fr"ORDER by controversial_score DESC"
  }

  private def idExists(id: ID[Post]): Fragment = fr"id = ${id} AND deleted = FALSE"

  private def idDeleted(id: ID[Post]): Fragment = fr"id = ${id} AND deleted = TRUE"

  override def paginate(
    channels: NonEmptySet[ID[Channel]],
    sortBy:   Post.Sorting,
    offset:   Long Refined NonNegative,
    limit:    Int Refined Positive
  ): F[Paginated[Post]] =
    (commonSelect ++ Fragments.whereAnd(Fragments.in(fr"channel_id", channels), fr"deleted = FALSE") ++ orderBy(sortBy))
      .paginate[PostDao](offset, limit)
      .map(_.map(_.toDomain))
      .transact(transactor)

  override def exists(id: ID[Post]): F[Boolean] =
    (fr"SELECT 1 FROM posts WHERE" ++ idExists(id)).exists.transact(transactor)

  override def deleted(id: ID[Post]): F[Boolean] =
    (fr"SELECT 1 FROM posts WHERE" ++ idDeleted(id)).exists.transact(transactor)

  override def getById(id: ID[Post], isDeleted: Boolean = false): F[Option[Post]] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[PostDao]
      .map(_.toDomain)
      .option
      .transact(transactor)

  override def requireById(id: ID[Post], isDeleted: Boolean = false): F[Post] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[PostDao]
      .map(_.toDomain)
      .failNotFound("Post", id)
      .transact(transactor)
}
