package io.branchtalk.discussions.writes

import io.branchtalk.discussions.dao.Channel
import io.branchtalk.shared.models.{ CreationScheduled, DeletionScheduled, RestoreScheduled, UpdateScheduled }

trait ChannelWrites[F[_]] {

  def createChannel(newChannel:       Channel.Create):  F[CreationScheduled[Channel]]
  def updateChannel(updatedChannel:   Channel.Update):  F[UpdateScheduled[Channel]]
  def deleteChannel(deletedChannel:   Channel.Delete):  F[DeletionScheduled[Channel]]
  def restoreChannel(restoredChannel: Channel.Restore): F[RestoreScheduled[Channel]]
}
