package io.branchtalk.users.writes

import cats.effect.Sync
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.model.{ CreationTime, ModificationTime, UUIDGenerator }
import io.branchtalk.users.events.{ BanCommandEvent, UsersCommandEvent }
import io.branchtalk.users.model.{ Ban, User }
import io.scalaland.chimney.dsl._

final class BanWritesImpl[F[_]: Sync: MDC](
  producer:   EventBusProducer[F, UsersCommandEvent],
  transactor: Transactor[F]
)(implicit
  uuidGenerator: UUIDGenerator
) extends Writes[F, User, UsersCommandEvent](producer)
    with BanWrites[F] {

  private val userCheck = new EntityCheck("User", transactor)

  override def orderBan(order: Ban.Order): F[Unit] = for {
    correlationID <- CorrelationID.getCurrentOrGenerate[F]
    id = order.bannedUserID
    _ <- userCheck(id, sql"""SELECT 1 FROM users WHERE id = ${id}""")
    now <- CreationTime.now[F]
    command = order
      .into[BanCommandEvent.OrderBan]
      .withFieldConst(_.createdAt, now)
      .withFieldConst(_.correlationID, correlationID)
      .transform
    _ <- postEvent(id, UsersCommandEvent.ForBan(command))
  } yield ()

  override def liftBan(lift: Ban.Lift): F[Unit] = for {
    correlationID <- CorrelationID.getCurrentOrGenerate[F]
    id = lift.bannedUserID
    _ <- userCheck(id, sql"""SELECT 1 FROM users WHERE id = ${id}""")
    now <- ModificationTime.now[F]
    command = lift
      .into[BanCommandEvent.LiftBan]
      .withFieldConst(_.modifiedAt, now)
      .withFieldConst(_.correlationID, correlationID)
      .transform
    _ <- postEvent(id, UsersCommandEvent.ForBan(command))
  } yield ()
}
