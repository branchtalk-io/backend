package io.branchtalk.discussions.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{ CommentCommandEvent, CommentEvent, DiscussionEvent, DiscussionsCommandEvent }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.scalaland.chimney.dsl._

final class CommentCommandHandler[F[_]: Sync] extends Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, DiscussionsCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionsCommandEvent.ForComment(command) =>
      command
    }.evalMap[F, (UUID, CommentEvent)] {
      case command: CommentCommandEvent.Create     => toCreate(command).widen
      case command: CommentCommandEvent.Update     => toUpdate(command).widen
      case command: CommentCommandEvent.Delete     => toDelete(command).widen
      case command: CommentCommandEvent.Restore    => toRestore(command).widen
      case command: CommentCommandEvent.Upvote     => toUpvote(command).widen
      case command: CommentCommandEvent.Downvote   => toDownvote(command).widen
      case command: CommentCommandEvent.RevokeVote => toRevokeVote(command).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForComment(value)
    }.handleErrorWith { error =>
      logger.error("Comment command processing failed", error)
      Stream.empty
    }

  def toCreate(command: CommentCommandEvent.Create): F[(UUID, CommentEvent.Created)] =
    (command.id.uuid -> command.transformInto[CommentEvent.Created]).pure[F]

  def toUpdate(command: CommentCommandEvent.Update): F[(UUID, CommentEvent.Updated)] =
    (command.id.uuid -> command.transformInto[CommentEvent.Updated]).pure[F]

  def toDelete(command: CommentCommandEvent.Delete): F[(UUID, CommentEvent.Deleted)] =
    (command.id.uuid -> command.transformInto[CommentEvent.Deleted]).pure[F]

  def toRestore(command: CommentCommandEvent.Restore): F[(UUID, CommentEvent.Restored)] =
    (command.id.uuid -> command.transformInto[CommentEvent.Restored]).pure[F]

  def toUpvote(command: CommentCommandEvent.Upvote): F[(UUID, CommentEvent.Upvoted)] =
    (command.id.uuid -> command.transformInto[CommentEvent.Upvoted]).pure[F]

  def toDownvote(command: CommentCommandEvent.Downvote): F[(UUID, CommentEvent.Downvoted)] =
    (command.id.uuid -> command.transformInto[CommentEvent.Downvoted]).pure[F]

  def toRevokeVote(command: CommentCommandEvent.RevokeVote): F[(UUID, CommentEvent.VoteRevoked)] =
    (command.id.uuid -> command.transformInto[CommentEvent.VoteRevoked]).pure[F]
}
