package io.branchtalk.discussions.writes

import io.branchtalk.discussions.model.Comment
import io.branchtalk.shared.model.{ CreationScheduled, DeletionScheduled, RestoreScheduled, UpdateScheduled }

trait CommentWrites[F[_]] {

  def createComment(newComment:      Comment.Create):  F[CreationScheduled[Comment]]
  def updateComment(updatedComment:  Comment.Update):  F[UpdateScheduled[Comment]]
  def deleteComment(updatedComment:  Comment.Delete):  F[DeletionScheduled[Comment]]
  def restoreComment(restoreComment: Comment.Restore): F[RestoreScheduled[Comment]]
}
