package io.branchtalk.discussions.reads

import cats.data.NonEmptySet
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.model.{ ID, Paginated }

trait PostReads[F[_]] {

  def paginate(
    channels: NonEmptySet[ID[Channel]],
    sortBy:   Post.Sorting,
    offset:   Paginated.Offset,
    limit:    Paginated.Limit
  ): F[Paginated[Post]]

  def exists(id: ID[Post]): F[Boolean]

  def deleted(id: ID[Post]): F[Boolean]

  def getById(id: ID[Post], isDeleted: Boolean = false): F[Option[Post]]

  def requireById(id: ID[Post], isDeleted: Boolean = false): F[Post]
}
