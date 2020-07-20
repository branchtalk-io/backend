package io.branchtalk.discussions.reads

import doobie.Transactor
import io.branchtalk.discussions.models.Channel
import io.branchtalk.shared.models

final class ChannelReadsImpl[F[_]](transactor: Transactor[F]) extends ChannelReads[F] {

  override def getById(id: models.ID[Channel]): F[Option[Channel]] = ???

  override def requireById(id: models.ID[Channel]): F[Channel] = ???
}
