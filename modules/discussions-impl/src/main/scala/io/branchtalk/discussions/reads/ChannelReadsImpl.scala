package io.branchtalk.discussions.reads

import cats.effect.Sync
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models._

final class ChannelReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends ChannelReads[F] {

  private implicit val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       url_name,
        |       name,
        |       description,
        |       created_at,
        |       last_modified_at
        |FROM channels""".stripMargin

  private def idExists(id: ID[Channel]): Fragment = fr"id = ${id} AND deleted = FALSE"

  private def idDeleted(id: ID[Channel]): Fragment = fr"id = ${id} AND deleted = TRUE"

  override def exists(id: ID[Channel]): F[Boolean] =
    (fr"SELECT 1 FROM channels WHERE" ++ idExists(id)).exists.transact(transactor)

  override def deleted(id: ID[Channel]): F[Boolean] =
    (fr"SELECT 1 FROM channels WHERE" ++ idDeleted(id)).exists.transact(transactor)

  override def getById(id: ID[Channel]): F[Option[Channel]] =
    (commonSelect ++ fr"WHERE" ++ idExists(id)).query[Channel].option.transact(transactor)

  override def requireById(id: ID[Channel]): F[Channel] =
    (commonSelect ++ fr"WHERE" ++ idExists(id)).query[Channel].failNotFound("User", id).transact(transactor)
}
