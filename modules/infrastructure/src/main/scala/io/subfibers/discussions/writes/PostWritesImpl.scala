package io.subfibers.discussions.writes

import cats.effect.{ Sync, Timer }
import cats.implicits._
import doobie._
import io.scalaland.chimney.dsl._
import io.subfibers.discussions.events.{ DiscussionCommandEvent, PostCommandEvent }
import io.subfibers.discussions.models.Post
import io.subfibers.shared.infrastructure.{ EventBusProducer, Writes }
import io.subfibers.shared.models._

class PostWritesImpl[F[_]: Sync: Timer](
  transactor: Transactor[F],
  publisher:  EventBusProducer[F, UUID, DiscussionCommandEvent]
) extends Writes[F, Post, DiscussionCommandEvent](transactor, publisher)
    with PostWrites[F] {

  override def createPost(newPost: Post.Create): F[CreationScheduled[Post]] =
    for {
      id <- UUID.create[F].map(ID[Post])
      now <- CreationTime.now[F]
      command = newPost
        .into[PostCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldConst(_.createdAt, now)
        .transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield CreationScheduled(id)

  override def updatePost(updatedPost: Post.Update): F[UpdateScheduled[Post]] =
    for {
      id <- updatedPost.id.pure[F]
      now <- ModificationTime.now[F]
      command = updatedPost.into[PostCommandEvent.Update].withFieldConst(_.modifiedAt, now).transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield UpdateScheduled(id)

  override def deletePost(deletedPost: Post.Delete): F[DeletionScheduled[Post]] =
    for {
      id <- deletedPost.id.pure[F]
      command = deletedPost.into[PostCommandEvent.Delete].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield DeletionScheduled(id)
}
