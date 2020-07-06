package io.subfibers.discussions.infrastructure

import cats.effect.{ Sync, Timer }
import cats.implicits._
import doobie._
import io.scalaland.chimney.dsl._
import io.subfibers.discussions.events.{ ChannelCommandEvent, DiscussionCommandEvent }
import io.subfibers.discussions.models.Channel
import io.subfibers.shared.infrastructure.{ EventBusProducer, Repository }
import io.subfibers.shared.models._

class ChannelRepositoryImpl[F[_]: Sync: Timer](
  transactor: Transactor[F],
  publisher:  EventBusProducer[F, UUID, DiscussionCommandEvent]
) extends Repository[F, Channel, DiscussionCommandEvent](transactor, publisher)
    with ChannelRepository[F] {

  override def createTopic(newChannel: Channel.Create): F[CreationScheduled[Channel]] =
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

  override def updateTopic(updatedChannel: Channel.Update): F[UpdateScheduled[Channel]] =
    for {
      id <- updatedChannel.id.pure[F]
      now <- ModificationTime.now[F]
      command = updatedChannel.into[ChannelCommandEvent.Update].withFieldConst(_.modifiedAt, now).transform
      _ <- postEvent(id, DiscussionCommandEvent.ForChannel(command))
    } yield UpdateScheduled(id)

  override def deleteTopic(deletedChannel: Channel.Delete): F[DeletionScheduled[Channel]] =
    for {
      id <- deletedChannel.id.pure[F]
      command = deletedChannel.into[ChannelCommandEvent.Delete].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForChannel(command))
    } yield DeletionScheduled(id)

  // TODO: define read models
}
