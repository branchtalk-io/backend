package io.subfibers.discussions.writes

import io.subfibers.discussions.models.Channel
import io.subfibers.shared.models.{ CreationScheduled, DeletionScheduled, UpdateScheduled }

trait ChannelWrites[F[_]] {

  def createTopic(newChannel:     Channel.Create): F[CreationScheduled[Channel]]
  def updateTopic(updatedChannel: Channel.Update): F[UpdateScheduled[Channel]]
  def deleteTopic(deletedChannel: Channel.Delete): F[DeletionScheduled[Channel]]
}
