package io.branchtalk.discussions.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{ DiscussionEvent, DiscussionsCommandEvent, PostCommandEvent, PostEvent }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.scalaland.chimney.dsl._

final class PostCommandHandler[F[_]: Sync] extends Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, DiscussionsCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionsCommandEvent.ForPost(command) =>
      command
    }.evalMap[F, (UUID, PostEvent)] {
      case command: PostCommandEvent.Create     => toCreate(command).widen
      case command: PostCommandEvent.Update     => toUpdate(command).widen
      case command: PostCommandEvent.Delete     => toDelete(command).widen
      case command: PostCommandEvent.Restore    => toRestore(command).widen
      case command: PostCommandEvent.Upvote     => toUpvote(command).widen
      case command: PostCommandEvent.Downvote   => toDownvote(command).widen
      case command: PostCommandEvent.RevokeVote => toRevokeVote(command).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForPost(value)
    }.handleErrorWith { error =>
      logger.error("Post command processing failed", error)
      Stream.empty
    }

  def toCreate(command: PostCommandEvent.Create): F[(UUID, PostEvent.Created)] =
    (command.id.uuid -> command.transformInto[PostEvent.Created]).pure[F]

  def toUpdate(command: PostCommandEvent.Update): F[(UUID, PostEvent.Updated)] =
    (command.id.uuid -> command.transformInto[PostEvent.Updated]).pure[F]

  def toDelete(command: PostCommandEvent.Delete): F[(UUID, PostEvent.Deleted)] =
    (command.id.uuid -> command.transformInto[PostEvent.Deleted]).pure[F]

  def toRestore(command: PostCommandEvent.Restore): F[(UUID, PostEvent.Restored)] =
    (command.id.uuid -> command.transformInto[PostEvent.Restored]).pure[F]

  def toUpvote(command: PostCommandEvent.Upvote): F[(UUID, PostEvent.Upvoted)] =
    (command.id.uuid -> command.transformInto[PostEvent.Upvoted]).pure[F]

  def toDownvote(command: PostCommandEvent.Downvote): F[(UUID, PostEvent.Downvoted)] =
    (command.id.uuid -> command.transformInto[PostEvent.Downvoted]).pure[F]

  def toRevokeVote(command: PostCommandEvent.RevokeVote): F[(UUID, PostEvent.VoteRevoked)] =
    (command.id.uuid -> command.transformInto[PostEvent.VoteRevoked]).pure[F]
}
