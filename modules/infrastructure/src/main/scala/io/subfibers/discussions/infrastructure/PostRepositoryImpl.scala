package io.subfibers.discussions.infrastructure

import cats.effect.Sync
import doobie._
import io.subfibers.discussions.events.DiscussionInternalEvent
import io.subfibers.discussions.models.Post
import io.subfibers.shared.infrastructure.{ EventBusPublisher, Repository }
import io.subfibers.shared.models._

class PostRepositoryImpl[F[_]: Sync](
  transactor: Transactor[F],
  publisher:  EventBusPublisher[F, UUID, DiscussionInternalEvent]
) extends Repository[F, Post, DiscussionInternalEvent](transactor, publisher)
    with PostRepository[F] {

  // TODO: translate to internal events and publish
  override def createPost(newPost:     Post.Create): F[CreationScheduled[Post]] = ???
  override def updatePost(updatedPost: Post.Update): F[UpdateScheduled[Post]]   = ???
  override def deletePost(deletedPost: Post.Delete): F[UpdateScheduled[Post]]   = ???

  // TODO: define read models
}
