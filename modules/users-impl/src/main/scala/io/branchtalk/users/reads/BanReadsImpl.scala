package io.branchtalk.users.reads

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.infrastructure.DoobieSupport.Fragments.whereAnd
import io.branchtalk.shared.model.ID
import io.branchtalk.users.infrastructure.DoobieExtensions.{ *, given }
import io.branchtalk.users.model.{ Ban, BanDao, Channel, User }

final class BanReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends BanReads[F] {

  private val channelBan: Ban.Scope.Type = Ban.Scope.Type.ForChannel
  private val globalBan:  Ban.Scope.Type = Ban.Scope.Type.Globally

  private val commonSelect: Fragment =
    fr"""SELECT user_id,
        |       ban_type,
        |       ban_id,
        |       reason
        |FROM bans""".stripMargin

  override def findForUser(userID: ID[User]): F[Set[Ban]] =
    (commonSelect ++ whereAnd(fr"user_id = $userID"))
      .queryWithLabel[BanDao](show"Require Users' Ban for User=$userID")
      .map(_.toDomain)
      .to[Set]
      .transact(transactor)

  override def findForChannel(channelID: ID[Channel]): F[Set[Ban]] =
    (commonSelect ++ whereAnd(fr"ban_id = $channelID", fr"ban_type = $channelBan"))
      .queryWithLabel[BanDao](show"Require Users' Ban for Channel=$channelID")
      .map(_.toDomain)
      .to[Set]
      .transact(transactor)

  override def findGlobally: F[Set[Ban]] =
    (commonSelect ++ whereAnd(fr"ban_type = $globalBan"))
      .queryWithLabel[BanDao]("Require Users' Ban globally")
      .map(_.toDomain)
      .to[Set]
      .transact(transactor)
}
