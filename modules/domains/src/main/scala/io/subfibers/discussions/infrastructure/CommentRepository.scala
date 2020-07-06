package io.subfibers.discussions.infrastructure

import io.subfibers.discussions.models.Comment
import io.subfibers.shared.models.{ CreationScheduled, DeletionScheduled, UpdateScheduled }

trait CommentRepository[F[_]] {

  def createComment(newComment:     Comment.Create): F[CreationScheduled[Comment]]
  def updateComment(updatedComment: Comment.Update): F[UpdateScheduled[Comment]]
  def deleteComment(deletedComment: Comment.Delete): F[DeletionScheduled[Comment]]

  // TODO: add read services
}
