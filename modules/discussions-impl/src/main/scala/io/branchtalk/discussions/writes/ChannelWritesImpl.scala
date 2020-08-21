package io.branchtalk.discussions.writes

import cats.effect.{ Sync, Timer }
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ ChannelCommandEvent, DiscussionCommandEvent }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.models._

final class ChannelWritesImpl[F[_]: Sync: Timer](publisher: EventBusProducer[F, DiscussionCommandEvent])(
  implicit uuidGenerator: UUIDGenerator
) extends Writes[F, Channel, DiscussionCommandEvent](publisher)
    with ChannelWrites[F] {

  override def createChannel(newChannel: Channel.Create): F[CreationScheduled[Channel]] =
    for {
      id <- UUID.create[F].map(ID[Channel])
      now <- CreationTime.now[F]
      command = newChannel
        .into[ChannelCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldConst(_.createdAt, now)
        .transform
      _ <- postEvent(id, DiscussionCommandEvent.ForChannel(command))
    } yield CreationScheduled(id)

  override def updateChannel(updatedChannel: Channel.Update): F[UpdateScheduled[Channel]] =
    for {
      id <- updatedChannel.id.pure[F]
      now <- ModificationTime.now[F]
      command = updatedChannel.into[ChannelCommandEvent.Update].withFieldConst(_.modifiedAt, now).transform
      _ <- postEvent(id, DiscussionCommandEvent.ForChannel(command))
    } yield UpdateScheduled(id)

  override def deleteChannel(deletedChannel: Channel.Delete): F[DeletionScheduled[Channel]] =
    for {
      id <- deletedChannel.id.pure[F]
      command = deletedChannel.into[ChannelCommandEvent.Delete].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForChannel(command))
    } yield DeletionScheduled(id)

  override def restoreChannel(restoredChannel: Channel.Restore): F[RestoreScheduled[Channel]] =
    for {
      id <- restoredChannel.id.pure[F]
      command = restoredChannel.into[ChannelCommandEvent.Restore].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForChannel(command))
    } yield RestoreScheduled(id)
}
