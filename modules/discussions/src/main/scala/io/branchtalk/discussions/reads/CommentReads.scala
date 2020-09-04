package io.branchtalk.discussions.reads

import io.branchtalk.discussions.model.Comment
import io.branchtalk.shared.models.ID

trait CommentReads[F[_]] {

  def exists(id: ID[Comment]): F[Boolean]

  def deleted(id: ID[Comment]): F[Boolean]

  def getById(id: ID[Comment]): F[Option[Comment]]

  def requireById(id: ID[Comment]): F[Comment]
}
