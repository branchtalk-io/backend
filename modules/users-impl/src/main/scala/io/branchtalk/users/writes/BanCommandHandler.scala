package io.branchtalk.users.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.events.{ BanCommandEvent, BanEvent, UsersCommandEvent, UsersEvent }
import io.scalaland.chimney.dsl._

final class BanCommandHandler[F[_]: Sync: MDC] extends Projector[F, UsersCommandEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, UsersCommandEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect { case UsersCommandEvent.ForBan(command) =>
      command
    }.evalMap[F, (UUID, BanEvent)] {
      case command: BanCommandEvent.OrderBan => toOrder(command).widen
      case command: BanCommandEvent.LiftBan  => toLift(command).widen
    }.map { case (key, value) =>
      key -> UsersEvent.ForBan(value)
    }.handleErrorWith { error =>
      logger.error("Ban command processing failed", error)
      Stream.empty
    }

  def toOrder(event: BanCommandEvent.OrderBan): F[(UUID, BanEvent.Banned)] =
    (event.bannedUserID.uuid -> event.transformInto[BanEvent.Banned]).pure[F]

  def toLift(event: BanCommandEvent.LiftBan): F[(UUID, BanEvent.Unbanned)] =
    (event.bannedUserID.uuid -> event.transformInto[BanEvent.Unbanned]).pure[F]
}
