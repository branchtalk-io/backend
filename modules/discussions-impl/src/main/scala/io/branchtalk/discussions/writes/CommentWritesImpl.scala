package io.branchtalk.discussions.writes

import cats.effect.{ Sync, Timer }
import io.branchtalk.discussions.events.{ CommentCommandEvent, DiscussionsCommandEvent }
import io.branchtalk.discussions.model.{ Channel, Comment, Post }
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model._
import io.scalaland.chimney.dsl._

final class CommentWritesImpl[F[_]: Sync: Timer: MDC](
  producer:   EventBusProducer[F, DiscussionsCommandEvent],
  transactor: Transactor[F]
)(implicit
  uuidGenerator: UUIDGenerator
) extends Writes[F, Comment, DiscussionsCommandEvent](producer)
    with CommentWrites[F] {

  private val postCheck    = new ParentCheck[Post]("Post", transactor)
  private val commentCheck = new EntityCheck("Comment", transactor)

  override def createComment(newComment: Comment.Create): F[CreationScheduled[Comment]] =
    for {
      channelID <- postCheck.withValue[ID[Channel]](
        newComment.postID,
        sql"""SELECT channel_id FROM posts WHERE id = ${newComment.postID} AND deleted = false"""
      )
      id <- ID.create[F, Comment]
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      now <- CreationTime.now[F]
      command = newComment
        .into[CommentCommandEvent.Create]
        .withFieldConst(_.channelID, channelID)
        .withFieldConst(_.id, id)
        .withFieldConst(_.createdAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForComment(command))
    } yield CreationScheduled(id)

  override def updateComment(updatedComment: Comment.Update): F[UpdateScheduled[Comment]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = updatedComment.id
      _ <- commentCheck(id, sql"""SELECT 1 FROM comments WHERE id = ${id} AND deleted = FALSE""")
      now <- ModificationTime.now[F]
      command = updatedComment
        .into[CommentCommandEvent.Update]
        .withFieldConst(_.modifiedAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForComment(command))
    } yield UpdateScheduled(id)

  override def deleteComment(deletedComment: Comment.Delete): F[DeletionScheduled[Comment]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = deletedComment.id
      _ <- commentCheck(id, sql"""SELECT 1 FROM comments WHERE id = ${id} AND deleted = FALSE""")
      command = deletedComment.into[CommentCommandEvent.Delete].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForComment(command))
    } yield DeletionScheduled(id)

  override def restoreComment(restoredComment: Comment.Restore): F[RestoreScheduled[Comment]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = restoredComment.id
      _ <- commentCheck(id, sql"""SELECT 1 FROM comments WHERE id = ${id} AND deleted = TRUE""")
      command = restoredComment
        .into[CommentCommandEvent.Restore]
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForComment(command))
    } yield RestoreScheduled(id)

  override def upvoteComment(vote: Comment.Upvote): F[Unit] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = vote.id
      _ <- commentCheck(id, sql"""SELECT 1 FROM comments WHERE id = ${id} AND deleted = FALSE""")
      command = vote.into[CommentCommandEvent.Upvote].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForComment(command))
    } yield ()

  override def downvoteComment(vote: Comment.Downvote): F[Unit] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = vote.id
      _ <- commentCheck(id, sql"""SELECT 1 FROM comments WHERE id = ${id} AND deleted = FALSE""")
      command = vote.into[CommentCommandEvent.Downvote].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForComment(command))
    } yield ()

  override def revokeCommentVote(vote: Comment.RevokeVote): F[Unit] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = vote.id
      _ <- commentCheck(id, sql"""SELECT 1 FROM comments WHERE id = ${id} AND deleted = FALSE""")
      command = vote.into[CommentCommandEvent.RevokeVote].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForComment(command))
    } yield ()
}
