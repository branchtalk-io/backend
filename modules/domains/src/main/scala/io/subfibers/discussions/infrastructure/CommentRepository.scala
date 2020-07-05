package io.subfibers.discussions.infrastructure

import io.subfibers.discussions.models.Comment
import io.subfibers.shared.models.{ CreationScheduled, UpdateScheduled }

trait CommentRepository[F[_]] {

  def createComment(newComment:     Comment.Create): F[CreationScheduled[Comment]]
  def updateComment(updatedComment: Comment.Update): F[UpdateScheduled[Comment]]
  def deleteComment(deletedComment: Comment.Delete): F[UpdateScheduled[Comment]]

  // TODO: add read services
}
