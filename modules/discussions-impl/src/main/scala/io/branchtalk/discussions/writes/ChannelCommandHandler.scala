package io.branchtalk.discussions.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{ ChannelCommandEvent, ChannelEvent, DiscussionEvent, DiscussionsCommandEvent }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.scalaland.chimney.dsl._

final class ChannelCommandHandler[F[_]: Sync] extends Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, DiscussionsCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionsCommandEvent.ForChannel(command) =>
      command
    }.evalMap[F, (UUID, ChannelEvent)] {
      case command: ChannelCommandEvent.Create  => toCreate(command).widen
      case command: ChannelCommandEvent.Update  => toUpdate(command).widen
      case command: ChannelCommandEvent.Delete  => toDelete(command).widen
      case command: ChannelCommandEvent.Restore => toRestore(command).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForChannel(value)
    }.handleErrorWith { error =>
      logger.error("Channel command processing failed", error)
      Stream.empty
    }

  def toCreate(command: ChannelCommandEvent.Create): F[(UUID, ChannelEvent.Created)] =
    (command.id.uuid -> command.transformInto[ChannelEvent.Created]).pure[F]

  def toUpdate(command: ChannelCommandEvent.Update): F[(UUID, ChannelEvent.Updated)] =
    (command.id.uuid -> command.transformInto[ChannelEvent.Updated]).pure[F]

  def toDelete(command: ChannelCommandEvent.Delete): F[(UUID, ChannelEvent.Deleted)] =
    (command.id.uuid -> command.transformInto[ChannelEvent.Deleted]).pure[F]

  def toRestore(command: ChannelCommandEvent.Restore): F[(UUID, ChannelEvent.Restored)] =
    (command.id.uuid -> command.transformInto[ChannelEvent.Restored]).pure[F]
}
