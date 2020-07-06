package io.subfibers.discussions.infrastructure

import cats.effect.{ Sync, Timer }
import cats.implicits._
import doobie._
import io.scalaland.chimney.dsl._
import io.subfibers.discussions.events.{ CommentCommandEvent, DiscussionCommandEvent }
import io.subfibers.discussions.models.Comment
import io.subfibers.shared.infrastructure.{ EventBusProducer, Repository }
import io.subfibers.shared.models._

class CommentRepositoryImpl[F[_]: Sync: Timer](
  transactor: Transactor[F],
  publisher:  EventBusProducer[F, UUID, DiscussionCommandEvent]
) extends Repository[F, Comment, DiscussionCommandEvent](transactor, publisher)
    with CommentRepository[F] {

  override def createComment(newComment: Comment.Create): F[CreationScheduled[Comment]] =
    for {
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

  // TODO: define read models
}
