package io.branchtalk.discussions.reads

import cats.data.NonEmptySet
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.model
import io.branchtalk.shared.model.{ ID, Paginated }

trait PostReads[F[_]] {

  def paginate(
    channels: NonEmptySet[model.ID[Channel]],
    offset:   Long Refined NonNegative,
    limit:    Int Refined Positive
  ): F[Paginated[Post]]

  def exists(id: ID[Post]): F[Boolean]

  def deleted(id: ID[Post]): F[Boolean]

  def getById(id: ID[Post], isDeleted: Boolean = false): F[Option[Post]]

  def requireById(id: ID[Post], isDeleted: Boolean = false): F[Post]
}
