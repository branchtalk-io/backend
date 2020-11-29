package io.branchtalk.discussions.reads

import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model._

final class ChannelReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends ChannelReads[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       url_name,
        |       name,
        |       description,
        |       created_at,
        |       last_modified_at
        |FROM channels""".stripMargin

  private val orderBy: Option[Channel.Sorting] => Fragment = {
    case Some(Channel.Sorting.Newest)         => fr"ORDER BY created_at DESC"
    case Some(Channel.Sorting.Alphabetically) => fr"ORDER BY name ASC"
    case None                                 => Fragment.empty
  }

  private def idExists(id: ID[Channel]): Fragment = fr"id = ${id} AND deleted = FALSE"

  private def idDeleted(id: ID[Channel]): Fragment = fr"id = ${id} AND deleted = TRUE"

  override def paginate(
    sortBy: Option[Channel.Sorting],
    offset: Long Refined NonNegative,
    limit:  Int Refined Positive
  ): F[Paginated[Channel]] =
    (commonSelect ++ Fragments.whereAnd(fr"deleted = FALSE") ++ orderBy(sortBy))
      .paginate[Channel](offset, limit)
      .transact(transactor)

  override def exists(id: ID[Channel]): F[Boolean] =
    (fr"SELECT 1 FROM channels WHERE" ++ idExists(id)).exists.transact(transactor)

  override def deleted(id: ID[Channel]): F[Boolean] =
    (fr"SELECT 1 FROM channels WHERE" ++ idDeleted(id)).exists.transact(transactor)

  override def getById(id: ID[Channel], isDeleted: Boolean = false): F[Option[Channel]] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[Channel]
      .option
      .transact(transactor)

  override def requireById(id: ID[Channel], isDeleted: Boolean = false): F[Channel] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .query[Channel]
      .failNotFound("User", id)
      .transact(transactor)
}
