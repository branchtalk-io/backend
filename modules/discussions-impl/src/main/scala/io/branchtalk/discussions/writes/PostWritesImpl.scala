package io.branchtalk.discussions.writes

import cats.effect.{ Sync, Timer }
import eu.timepit.refined.refineV
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, PostCommandEvent }
import io.branchtalk.discussions.model.Post
import io.branchtalk.shared.infrastructure.{ EventBusProducer, NormalizeForUrl, Writes }
import io.branchtalk.shared.models._

final class PostWritesImpl[F[_]: Sync: Timer](producer: EventBusProducer[F, DiscussionCommandEvent])(
  implicit uuidGenerator: UUIDGenerator
) extends Writes[F, Post, DiscussionCommandEvent](producer)
    with PostWrites[F] {

  override def createPost(newPost: Post.Create): F[CreationScheduled[Post]] =
    for {
      id <- UUID.create[F].map(ID[Post])
      now <- CreationTime.now[F]
      command = newPost
        .into[PostCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldConst(
          _.urlTitle,
          Post.UrlTitle(refineV[NonEmpty](NormalizeForUrl(newPost.title.value.value)).getOrElse("post": NonEmptyString))
        )
        .withFieldConst(_.createdAt, now)
        .transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield CreationScheduled(id)

  override def updatePost(updatedPost: Post.Update): F[UpdateScheduled[Post]] =
    for {
      id <- updatedPost.id.pure[F]
      now <- ModificationTime.now[F]
      command = updatedPost
        .into[PostCommandEvent.Update]
        .withFieldConst(
          _.newUrlTitle,
          updatedPost.newTitle.map { title: Post.Title =>
            Post.UrlTitle(refineV[NonEmpty](NormalizeForUrl(title.value.value)).getOrElse("post": NonEmptyString))
          }
        )
        .withFieldConst(_.modifiedAt, now)
        .transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield UpdateScheduled(id)

  override def deletePost(deletedPost: Post.Delete): F[DeletionScheduled[Post]] =
    for {
      id <- deletedPost.id.pure[F]
      command = deletedPost.into[PostCommandEvent.Delete].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield DeletionScheduled(id)

  override def restorePost(restoredPost: Post.Restore): F[RestoreScheduled[Post]] =
    for {
      id <- restoredPost.id.pure[F]
      command = restoredPost.into[PostCommandEvent.Restore].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield RestoreScheduled(id)
}
