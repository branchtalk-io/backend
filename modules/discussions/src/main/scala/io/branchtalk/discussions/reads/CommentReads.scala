package io.branchtalk.discussions.reads

import io.branchtalk.discussions.dao.{ Channel, Comment }
import io.branchtalk.shared.models.ID

trait CommentReads[F[_]] {

  def exists(id: ID[Comment]): F[Boolean]

  def getById(id: ID[Comment]): F[Option[Comment]]

  def requireById(id: ID[Comment]): F[Comment]
}
