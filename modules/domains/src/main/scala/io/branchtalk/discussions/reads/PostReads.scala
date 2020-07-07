package io.branchtalk.discussions.reads

import io.branchtalk.discussions.models.Post
import io.branchtalk.shared.models.ID

trait PostReads[F[_]] {
  def getById(id: ID[Post]): F[Option[Post]]

  def requireById(id: ID[Post]): F[Post]
}
