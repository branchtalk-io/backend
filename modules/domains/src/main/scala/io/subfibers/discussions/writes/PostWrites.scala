package io.subfibers.discussions.writes

import io.subfibers.discussions.models._
import io.subfibers.shared.models._

trait PostWrites[F[_]] {

  def createPost(newPost:     Post.Create): F[CreationScheduled[Post]]
  def updatePost(updatedPost: Post.Update): F[UpdateScheduled[Post]]
  def deletePost(deletedPost: Post.Delete): F[DeletionScheduled[Post]]
}
