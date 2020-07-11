package io.branchtalk.discussions.reads

import io.branchtalk.discussions.models.Comment
import io.branchtalk.shared.models.ID

trait CommentReads[F[_]] {
  def getById(id: ID[Comment]): F[Option[Comment]]

  def requireById(id: ID[Comment]): F[Comment]
}
