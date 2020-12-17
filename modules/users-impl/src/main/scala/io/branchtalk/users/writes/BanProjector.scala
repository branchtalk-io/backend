package io.branchtalk.users.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.events.{ BanCommandEvent, BanEvent, UsersCommandEvent, UsersEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.Ban
import io.scalaland.chimney.dsl._

final class BanProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, UsersCommandEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, UsersCommandEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect { case UsersCommandEvent.ForBan(event) =>
      event
    }.evalMap[F, (UUID, BanEvent)] {
      case event: BanCommandEvent.OrderBan => toOrder(event).widen
      case event: BanCommandEvent.LiftBan  => toLift(event).widen
    }.map { case (key, value) =>
      key -> UsersEvent.ForBans(value)
    }.handleErrorWith { error =>
      logger.error("Ban event processing failed", error)
      Stream.empty
    }

  def toOrder(event: BanCommandEvent.OrderBan): F[(UUID, BanEvent.Banned)] = {
    val Ban.Scope.Tupled(banType, banID) = event.scope
    sql"""INSERT INTO bans (
         |  user_id,
         |  ban_type,
         |  ban_id,
         |  reason
         |)
         |VALUES (
         |  ${event.bannedUserID},
         |  ${banType},
         |  ${banID},
         |  ${event.reason}
         |)""".stripMargin.update.run.transact(transactor) >>
      (event.bannedUserID.uuid -> event.transformInto[BanEvent.Banned]).pure[F]
  }

  def toLift(event: BanCommandEvent.LiftBan): F[(UUID, BanEvent.Unbanned)] = {
    val Ban.Scope.Tupled(banType, banID) = event.scope
    sql"""DELETE FROM bans
         |WHERE user_id  = ${event.bannedUserID}
         |  AND ban_type = ${banType}
         |  AND ban_id   = ${banID}""".stripMargin.update.run.transact(transactor) >>
      (event.bannedUserID.uuid -> event.transformInto[BanEvent.Unbanned]).pure[F]
  }
}
