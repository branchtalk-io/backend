package io.branchtalk.discussions.writes

import io.branchtalk.discussions.models.Comment
import io.branchtalk.shared.models.{ CreationScheduled, DeletionScheduled, UpdateScheduled }

trait CommentWrites[F[_]] {

  def createComment(newComment:     Comment.Create): F[CreationScheduled[Comment]]
  def updateComment(updatedComment: Comment.Update): F[UpdateScheduled[Comment]]
  def deleteComment(deletedComment: Comment.Delete): F[DeletionScheduled[Comment]]
}
