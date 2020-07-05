package io.subfibers.discussions.infrastructure

import cats.effect.Sync
import doobie._
import io.subfibers.discussions.events.DiscussionInternalEvent
import io.subfibers.discussions.models.Comment
import io.subfibers.shared.infrastructure.{ EventBusPublisher, Repository }
import io.subfibers.shared.models._

class CommentRepositoryImpl[F[_]: Sync](
  transactor: Transactor[F],
  publisher:  EventBusPublisher[F, UUID, DiscussionInternalEvent]
) extends Repository[F, Comment, DiscussionInternalEvent](transactor, publisher)
    with CommentRepository[F] {

  // TODO: translate to internal events and publish
  override def createComment(newComment:     Comment.Create): F[CreationScheduled[Comment]] = ???
  override def updateComment(updatedComment: Comment.Update): F[UpdateScheduled[Comment]]   = ???
  override def deleteComment(deletedComment: Comment.Delete): F[UpdateScheduled[Comment]]   = ???

  // TODO: define read models
}
