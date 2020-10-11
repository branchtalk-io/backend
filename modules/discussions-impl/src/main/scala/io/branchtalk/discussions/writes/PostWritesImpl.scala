package io.branchtalk.discussions.writes

import cats.effect.{ Sync, Timer }
import eu.timepit.refined.refineV
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, PostCommandEvent }
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.infrastructure.{ EventBusProducer, NormalizeForUrl, Writes }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models._

final class PostWritesImpl[F[_]: Sync: Timer](
  producer:   EventBusProducer[F, DiscussionCommandEvent],
  transactor: Transactor[F]
)(
  implicit uuidGenerator: UUIDGenerator
) extends Writes[F, Post, DiscussionCommandEvent](producer)
    with PostWrites[F] {

  private val channelCheck = new ParentCheck[Channel]("Channel", transactor)
  private val postCheck    = new EntityCheck("Post", transactor)

  override def createPost(newPost: Post.Create): F[CreationScheduled[Post]] =
    for {
      _ <- channelCheck(newPost.channelID,
                        sql"""SELECT 1 FROM channels WHERE id = ${newPost.channelID} AND deleted = false""")
      id <- ID.create[F, Post]
      now <- CreationTime.now[F]
      command = newPost
        .into[PostCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldConst(
          _.urlTitle,
          Post.UrlTitle(
            refineV[NonEmpty](NormalizeForUrl(newPost.title.nonEmptyString.value)).getOrElse("post": NonEmptyString)
          )
        )
        .withFieldConst(_.createdAt, now)
        .transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield CreationScheduled(id)

  override def updatePost(updatedPost: Post.Update): F[UpdateScheduled[Post]] =
    for {
      id <- updatedPost.id.pure[F]
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = ${id} AND deleted = FALSE""")
      now <- ModificationTime.now[F]
      command = updatedPost
        .into[PostCommandEvent.Update]
        .withFieldConst(
          _.newUrlTitle,
          updatedPost.newTitle.map { title: Post.Title =>
            Post.UrlTitle(
              refineV[NonEmpty](NormalizeForUrl(title.nonEmptyString.value)).getOrElse("post": NonEmptyString)
            )
          }
        )
        .withFieldConst(_.modifiedAt, now)
        .transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield UpdateScheduled(id)

  override def deletePost(deletedPost: Post.Delete): F[DeletionScheduled[Post]] =
    for {
      id <- deletedPost.id.pure[F]
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = ${id} AND deleted = FALSE""")
      command = deletedPost.into[PostCommandEvent.Delete].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield DeletionScheduled(id)

  override def restorePost(restoredPost: Post.Restore): F[RestoreScheduled[Post]] =
    for {
      id <- restoredPost.id.pure[F]
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = ${id} AND deleted = TRUE""")
      command = restoredPost.into[PostCommandEvent.Restore].transform
      _ <- postEvent(id, DiscussionCommandEvent.ForPost(command))
    } yield RestoreScheduled(id)
}
