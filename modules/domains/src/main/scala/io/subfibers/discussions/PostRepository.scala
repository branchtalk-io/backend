package io.subfibers.discussions

import io.subfibers._

trait PostRepository[F[_]] {

  def createPost(newPost:     NewPost):    F[CreationScheduled[Post]]
  def updatePost(updatedPost: UpdatePost): F[UpdateScheduled[Post]]
  def deletePost(id:          ID[Post]):   F[UpdateScheduled[Post]]
}
