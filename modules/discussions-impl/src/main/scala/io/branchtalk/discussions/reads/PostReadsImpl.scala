package io.branchtalk.discussions.reads

import cats.data.NonEmptySet
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.discussions.infrastructure.DoobieExtensions._
import io.branchtalk.discussions.model.{ Channel, Post, PostDao }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model
import io.branchtalk.shared.model.Paginated

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
        |       comments_nr
        |FROM posts""".stripMargin

  private def idExists(id: model.ID[Post]): Fragment = fr"id = ${id} AND deleted = FALSE"

  private def idDeleted(id: model.ID[Post]): Fragment = fr"id = ${id} AND deleted = TRUE"

  override def paginate(
    channels: NonEmptySet[model.ID[Channel]],
    offset:   Long Refined NonNegative,
    limit:    Int Refined Positive
  ): F[Paginated[Post]] =
    (commonSelect ++ Fragments.whereAnd(Fragments.in(fr"channel_id", channels), fr"deleted = FALSE"))
      .paginate[PostDao](offset, limit)
      .map(_.map(_.toDomain))
      .transact(transactor)

  override def exists(id: model.ID[Post]): F[Boolean] =
    (fr"SELECT 1 FROM posts WHERE" ++ idExists(id)).exists.transact(transactor)

  override def deleted(id: model.ID[Post]): F[Boolean] =
    (fr"SELECT 1 FROM posts WHERE" ++ idDeleted(id)).exists.transact(transactor)

  override def getById(id: model.ID[Post], isDeleted: Boolean = false): F[Option[Post]] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[PostDao]
      .map(_.toDomain)
      .option
      .transact(transactor)

  override def requireById(id: model.ID[Post], isDeleted: Boolean = false): F[Post] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[PostDao]
      .map(_.toDomain)
      .failNotFound("Post", id)
      .transact(transactor)
}
