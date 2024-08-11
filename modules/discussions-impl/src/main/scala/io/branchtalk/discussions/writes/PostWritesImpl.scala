package io.branchtalk.discussions.writes

import cats.effect.Sync
import io.branchtalk.discussions.events.{ DiscussionsCommandEvent, PostCommandEvent }
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.infrastructure.{ KafkaEventBus, NormalizeForUrl, Writes }
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.*
import io.scalaland.chimney.dsl.*

final class PostWritesImpl[F[_]: Sync: MDC](
  producer:   KafkaEventBus.Producer[F, DiscussionsCommandEvent],
  transactor: Transactor[F]
)(using UUID.Generator)
    extends Writes[F, Post, DiscussionsCommandEvent](producer),
      PostWrites[F] {

  private val channelCheck = new ParentCheck[Channel]("Channel", transactor)
  private val postCheck    = new EntityCheck("Post", transactor)

  private def titleToUrlTitle(title: Post.Title): F[Post.UrlTitle] =
    ParseNewtype[F].parse[Post.UrlTitle](NormalizeForUrl(title.unwrap))

  override def createPost(newPost: Post.Create): F[CreationScheduled[Post]] =
    for {
      _ <- channelCheck(newPost.channelID,
                        sql"""SELECT 1 FROM channels WHERE id = ${newPost.channelID} AND deleted = false"""
      )
      id <- ID.create[F, Post]
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      now <- CreationTime.now[F]
      urlTitle <- titleToUrlTitle(newPost.title)
      command = newPost
        .into[PostCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldConst(_.urlTitle, urlTitle)
        .withFieldConst(_.createdAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForPost(command))
    } yield CreationScheduled(id)

  override def updatePost(updatedPost: Post.Update): F[UpdateScheduled[Post]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = updatedPost.id
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = $id AND deleted = FALSE""")
      now <- ModificationTime.now[F]
      newUrlTitle <- updatedPost.newTitle.traverse(titleToUrlTitle)
      command = updatedPost
        .into[PostCommandEvent.Update]
        .withFieldConst(_.newUrlTitle, newUrlTitle)
        .withFieldConst(_.modifiedAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForPost(command))
    } yield UpdateScheduled(id)

  override def deletePost(deletedPost: Post.Delete): F[DeletionScheduled[Post]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = deletedPost.id
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = $id AND deleted = FALSE""")
      command = deletedPost.into[PostCommandEvent.Delete].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForPost(command))
    } yield DeletionScheduled(id)

  override def restorePost(restoredPost: Post.Restore): F[RestoreScheduled[Post]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = restoredPost.id
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = $id AND deleted = TRUE""")
      command = restoredPost.into[PostCommandEvent.Restore].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForPost(command))
    } yield RestoreScheduled(id)

  override def upvotePost(vote: Post.Upvote): F[Unit] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = vote.id
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = $id AND deleted = FALSE""")
      command = vote.into[PostCommandEvent.Upvote].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForPost(command))
    } yield ()

  override def downvotePost(vote: Post.Downvote): F[Unit] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = vote.id
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = $id AND deleted = FALSE""")
      command = vote.into[PostCommandEvent.Downvote].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForPost(command))
    } yield ()

  override def revokePostVote(vote: Post.RevokeVote): F[Unit] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = vote.id
      _ <- postCheck(id, sql"""SELECT 1 FROM posts WHERE id = $id AND deleted = FALSE""")
      command = vote.into[PostCommandEvent.RevokeVote].withFieldConst(_.correlationID, correlationID).transform
      _ <- postEvent(id, DiscussionsCommandEvent.ForPost(command))
    } yield ()
}
