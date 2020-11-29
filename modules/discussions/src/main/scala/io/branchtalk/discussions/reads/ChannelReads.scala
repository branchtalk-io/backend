package io.branchtalk.discussions.reads

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.{ ID, Paginated }

trait ChannelReads[F[_]] {

  def paginate(
    sortBy: Channel.Sorting,
    offset: Long Refined NonNegative,
    limit:  Int Refined Positive
  ): F[Paginated[Channel]]

  def exists(id: ID[Channel]): F[Boolean]

  def deleted(id: ID[Channel]): F[Boolean]

  def getById(id: ID[Channel], isDeleted: Boolean = false): F[Option[Channel]]

  def requireById(id: ID[Channel], isDeleted: Boolean = false): F[Channel]
}
