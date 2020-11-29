package io.branchtalk.discussions.reads

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.discussions.model.{ Comment, Post }
import io.branchtalk.shared.model.{ ID, Paginated }

trait CommentReads[F[_]] {

  def paginate(
    post:      ID[Post],
    repliesTo: Option[ID[Comment]],
    sorting:   Comment.Sorting,
    offset:    Long Refined NonNegative,
    limit:     Int Refined Positive
  ): F[Paginated[Comment]]

  def exists(id: ID[Comment]): F[Boolean]

  def deleted(id: ID[Comment]): F[Boolean]

  def getById(id: ID[Comment], isDeleted: Boolean = false): F[Option[Comment]]

  def requireById(id: ID[Comment], isDeleted: Boolean = false): F[Comment]
}
