package io.branchtalk.discussions.reads

import io.branchtalk.discussions.model.{ Comment, Post }
import io.branchtalk.shared.model.{ ID, Paginated }

trait CommentReads[F[_]] {

  def paginate(
    post:      ID[Post],
    repliesTo: Option[ID[Comment]],
    sorting:   Comment.Sorting,
    offset:    Paginated.Offset,
    limit:     Paginated.Limit
  ): F[Paginated[Comment]]

  def exists(id: ID[Comment]): F[Boolean]

  def deleted(id: ID[Comment]): F[Boolean]

  def getById(id: ID[Comment], isDeleted: Boolean = false): F[Option[Comment]]

  def requireById(id: ID[Comment], isDeleted: Boolean = false): F[Comment]
}
