package io.branchtalk.users.reads

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model.ID
import io.branchtalk.users.model.{ Ban, BanDao, User }

final class BanReadsImpl[F[_]: Sync](
  transactor: Transactor[F]
) extends BanReads[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT user_id,
        |       ban_type,
        |       ban_id,
        |       reason
        |FROM bans""".stripMargin

  override def findForUser(userID: ID[User]): F[Set[Ban]] =
    (commonSelect ++ fr"WHERE user_id = $userID").query[BanDao].map(_.toDomain).to[Set].transact(transactor)
}
