package io.branchtalk.discussions.reads

import io.branchtalk.discussions.model.Comment
import io.branchtalk.shared.model.ID

trait CommentReads[F[_]] {

  def exists(id: ID[Comment]): F[Boolean]

  def deleted(id: ID[Comment]): F[Boolean]

  def getById(id: ID[Comment], isDeleted: Boolean = false): F[Option[Comment]]

  def requireById(id: ID[Comment], isDeleted: Boolean = false): F[Comment]
}
