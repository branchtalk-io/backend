package io.branchtalk.discussions.reads

import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.models.ID

trait ChannelReads[F[_]] {

  def exists(id: ID[Channel]): F[Boolean]

  def getById(id: ID[Channel]): F[Option[Channel]]

  def requireById(id: ID[Channel]): F[Channel]
}
