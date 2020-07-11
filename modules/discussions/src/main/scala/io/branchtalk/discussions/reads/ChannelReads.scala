package io.branchtalk.discussions.reads

import io.branchtalk.discussions.models.Channel
import io.branchtalk.shared.models.ID

trait ChannelReads[F[_]] {
  def getById(id: ID[Channel]): F[Option[Channel]]

  def requireById(id: ID[Channel]): F[Channel]
}
