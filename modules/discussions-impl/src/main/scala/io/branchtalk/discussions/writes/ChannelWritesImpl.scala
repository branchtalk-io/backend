package io.branchtalk.discussions.writes

import cats.effect.Sync
import io.branchtalk.discussions.events.{ ChannelCommandEvent, DiscussionsCommandEvent }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model._
import io.scalaland.chimney.dsl._

final class ChannelWritesImpl[F[_]: Sync: MDC](
  producer:   EventBusProducer[F, DiscussionsCommandEvent],
  transactor: Transactor[F]
)(implicit
  uuidGenerator: UUIDGenerator
) extends Writes[F, Channel, DiscussionsCommandEvent](producer)
    with ChannelWrites[F] {

  private val channelCheck = new EntityCheck("Channel", transactor)

  override def createChannel(newChannel: Channel.Create): F[CreationScheduled[Channel]] =
    for {
      id <- ID.create[F, Channel]
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      now <- CreationTime.now[F]
      command = newChannel
        .into[ChannelCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldConst(_.createdAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForChannel(command))
    } yield CreationScheduled(id)

  override def updateChannel(updatedChannel: Channel.Update): F[UpdateScheduled[Channel]] =
    for {
      id <- updatedChannel.id.pure[F]
      _ <- channelCheck(id, sql"""SELECT 1 FROM channels WHERE id = ${id} AND deleted = FALSE""")
      now <- ModificationTime.now[F]
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      command = updatedChannel
        .into[ChannelCommandEvent.Update]
        .withFieldConst(_.modifiedAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForChannel(command))
    } yield UpdateScheduled(id)

  override def deleteChannel(deletedChannel: Channel.Delete): F[DeletionScheduled[Channel]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = deletedChannel.id
      _ <- channelCheck(id, sql"""SELECT 1 FROM channels WHERE id = ${id} AND deleted = FALSE""")
      command = deletedChannel.into[ChannelCommandEvent.Delete].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForChannel(command))
    } yield DeletionScheduled(id)

  override def restoreChannel(restoredChannel: Channel.Restore): F[RestoreScheduled[Channel]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = restoredChannel.id
      _ <- channelCheck(id, sql"""SELECT 1 FROM channels WHERE id = ${id} AND deleted = TRUE""")
      command = restoredChannel
        .into[ChannelCommandEvent.Restore]
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForChannel(command))
    } yield RestoreScheduled(id)
}
