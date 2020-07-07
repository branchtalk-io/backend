package io.branchtalk.discussions.writes

import io.branchtalk.discussions.models.Channel
import io.branchtalk.shared.models.{ CreationScheduled, DeletionScheduled, UpdateScheduled }

trait ChannelWrites[F[_]] {

  def createTopic(newChannel:     Channel.Create): F[CreationScheduled[Channel]]
  def updateTopic(updatedChannel: Channel.Update): F[UpdateScheduled[Channel]]
  def deleteTopic(deletedChannel: Channel.Delete): F[DeletionScheduled[Channel]]
}
