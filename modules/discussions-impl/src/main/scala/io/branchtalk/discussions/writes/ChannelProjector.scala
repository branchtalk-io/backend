package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{ ChannelCommandEvent, ChannelEvent, DiscussionEvent, DiscussionsCommandEvent }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.scalaland.chimney.dsl._

final class ChannelProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionsCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionsCommandEvent.ForChannel(event) =>
      event
    }.evalMap[F, (UUID, ChannelEvent)] {
      case event: ChannelCommandEvent.Create  => toCreate(event).widen
      case event: ChannelCommandEvent.Update  => toUpdate(event).widen
      case event: ChannelCommandEvent.Delete  => toDelete(event).widen
      case event: ChannelCommandEvent.Restore => toRestore(event).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForChannel(value)
    }.handleErrorWith { error =>
      logger.error("Channel event processing failed", error)
      Stream.empty
    }

  def toCreate(event: ChannelCommandEvent.Create): F[(UUID, ChannelEvent.Created)] =
    withCorrelationID(event.correlationID) {
      sql"""INSERT INTO channels (
           |  id,
           |  url_name,
           |  name,
           |  description,
           |  created_at
           |)
           |VALUES (
           |  ${event.id},
           |  ${event.urlName},
           |  ${event.name},
           |  ${event.description},
           |  ${event.createdAt}
           |)
           |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[ChannelEvent.Created])
    }

  def toUpdate(event: ChannelCommandEvent.Update): F[(UUID, ChannelEvent.Updated)] =
    withCorrelationID(event.correlationID) {
      NonEmptyList
        .fromList(
          List(
            event.newUrlName.toUpdateFragment(fr"url_name"),
            event.newName.toUpdateFragment(fr"name"),
            event.newDescription.toUpdateFragment(fr"description")
          ).flatten
        )
        .fold(
          Sync[F].delay(logger.warn(show"Channel update ignored as it doesn't contain any modification:\n$event"))
        )(updates =>
          (fr"UPDATE channels SET" ++
            (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
            fr"WHERE id = ${event.id}").update.run.transact(transactor).void
        )
        .as(event.id.uuid -> event.transformInto[ChannelEvent.Updated])
    }

  def toDelete(event: ChannelCommandEvent.Delete): F[(UUID, ChannelEvent.Deleted)] =
    withCorrelationID(event.correlationID) {
      sql"UPDATE channels SET deleted = TRUE WHERE id = ${event.id}".update.run
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[ChannelEvent.Deleted])
    }

  def toRestore(event: ChannelCommandEvent.Restore): F[(UUID, ChannelEvent.Restored)] =
    withCorrelationID(event.correlationID) {
      sql"UPDATE channels SET deleted = FALSE WHERE id = ${event.id}".update.run
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[ChannelEvent.Restored])
    }
}
