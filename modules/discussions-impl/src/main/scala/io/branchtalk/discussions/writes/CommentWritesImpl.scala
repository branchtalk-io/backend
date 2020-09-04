package io.branchtalk.discussions.writes

import cats.effect.{ Sync, Timer }
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ CommentCommandEvent, DiscussionCommandEvent }
import io.branchtalk.discussions.model.{ Comment, Post }
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models._

final class CommentWritesImpl[F[_]: Sync: Timer](
  producer:   EventBusProducer[F, DiscussionCommandEvent],
  transactor: Transactor[F]
)(
  implicit uuidGenerator: UUIDGenerator
) extends Writes[F, Comment, DiscussionCommandEvent](producer)
    with CommentWrites[F] {

  private val postCheck = new ParentCheck[Post]("Post", transactor)

  override def createComment(newComment: Comment.Create): F[CreationScheduled[Comment]] =
    for {
      _ <- postCheck(newComment.postID,
                     sql"""SELECT 1 FROM posts WHERE id = ${newComment.postID} AND deleted = false""")
      id <- UUID.create[F].map(ID[Comment])
      now <- CreationTime.now[F]
      command = newComment
        .into[CommentCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldConst(_.createdAt, now)
        .transform
      _ <- postEvent(id, DiscussionCommandEvent.ForComment(command))
    } yield CreationScheduled(id)

  override def updateComment(updatedComment: Comment.Update): F[UpdateScheduled[Comment]] =
    for {
      id <- updatedComment.id.pure[F]
      now <- ModificationTime.now[F]
      command = updatedComment.into[CommentCommandEvent.Update].withFieldConst(_.modifiedAt, now).transform
      _ <- postEvent(id, DiscussionCommandEvent.ForComment(command))
    } yield UpdateScheduled(id)

  override def deleteComment(deletedComment: Comment.Delete): F[DeletionScheduled[Comment]] =
    for {
      id <- deletedComment.id.pure[F]
      command = deletedComment.into[CommentCommandEvent.Delete].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForComment(command))
    } yield DeletionScheduled(id)

  override def restoreComment(restoredComment: Comment.Restore): F[RestoreScheduled[Comment]] =
    for {
      id <- restoredComment.id.pure[F]
      command = restoredComment.into[CommentCommandEvent.Restore].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForComment(command))
    } yield RestoreScheduled(id)
}
