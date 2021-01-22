package io.branchtalk.users.reads

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.DoobieSupport.Fragments.whereAnd
import io.branchtalk.shared.model.ID
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Ban, BanDao, Channel, User }

final class BanReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends BanReads[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val channelBan: Ban.Scope.Type = Ban.Scope.Type.ForChannel
  private val globalBan:  Ban.Scope.Type = Ban.Scope.Type.Globally

  private val commonSelect: Fragment =
    fr"""SELECT user_id,
        |       ban_type,
        |       ban_id,
        |       reason
        |FROM bans""".stripMargin

  override def findForUser(userID: ID[User]): F[Set[Ban]] =
    (commonSelect ++ whereAnd(fr"user_id = $userID")).query[BanDao].map(_.toDomain).to[Set].transact(transactor)

  override def findForChannel(channelID: ID[Channel]): F[Set[Ban]] =
    (commonSelect ++ whereAnd(fr"ban_id = $channelID", fr"ban_type = $channelBan"))
      .query[BanDao]
      .map(_.toDomain)
      .to[Set]
      .transact(transactor)

  override def findGlobally: F[Set[Ban]] =
    (commonSelect ++ whereAnd(fr"ban_type = $globalBan")).query[BanDao].map(_.toDomain).to[Set].transact(transactor)
}
