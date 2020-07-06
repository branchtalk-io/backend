package io.subfibers.discussions.infrastructure

import io.subfibers.discussions.models.Channel
import io.subfibers.shared.models.{ CreationScheduled, DeletionScheduled, UpdateScheduled }

trait ChannelRepository[F[_]] {

  def createTopic(newChannel:     Channel.Create): F[CreationScheduled[Channel]]
  def updateTopic(updatedChannel: Channel.Update): F[UpdateScheduled[Channel]]
  def deleteTopic(deletedChannel: Channel.Delete): F[DeletionScheduled[Channel]]

  // TODO: add read services
}
