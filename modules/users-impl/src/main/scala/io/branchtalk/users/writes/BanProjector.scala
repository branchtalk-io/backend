package io.branchtalk.users.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.events.{ BanEvent, UsersEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.Ban
import io.branchtalk.users.model.BanProperties.Scope
import io.scalaland.chimney.dsl._

final class BanProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, UsersEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, UsersEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect { case UsersEvent.ForBan(event) =>
      event
    }.evalMap[F, (UUID, BanEvent)] {
      case event: BanEvent.Banned   => toOrder(event).widen
      case event: BanEvent.Unbanned => toLift(event).widen
    }.map { case (key, value) =>
      key -> UsersEvent.ForBan(value)
    }.handleErrorWith { error =>
      logger.error("Ban event processing failed", error)
      Stream.empty
    }

  def toOrder(event: BanEvent.Banned): F[(UUID, BanEvent.Banned)] =
    withCorrelationID(event.correlationID) {
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
           |)""".stripMargin.update.run
        .transact(transactor)
        .as(event.bannedUserID.uuid -> event.transformInto[BanEvent.Banned])
    }

  def toLift(event: BanEvent.Unbanned): F[(UUID, BanEvent.Unbanned)] =
    withCorrelationID(event.correlationID) {
      val Ban.Scope.Tupled(banType, _) = event.scope
      (event.scope match {
        case Scope.ForChannel(channelID) =>
          sql"""DELETE FROM bans
               |WHERE user_id  = ${event.bannedUserID}
               |  AND ban_type = $banType
               |  AND ban_id   = $channelID""".stripMargin
        case Scope.Globally =>
          sql"""DELETE FROM bans
               |WHERE user_id  = ${event.bannedUserID}
               |  AND ban_type = $banType""".stripMargin
      }).update.run.transact(transactor).as(event.bannedUserID.uuid -> event.transformInto[BanEvent.Unbanned])
    }
}
