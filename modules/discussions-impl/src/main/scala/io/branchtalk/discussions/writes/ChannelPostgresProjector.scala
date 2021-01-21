package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{ ChannelEvent, DiscussionEvent }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID

final class ChannelPostgresProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, DiscussionEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionEvent.ForChannel(event) =>
      event
    }.evalMap[F, (UUID, ChannelEvent)] {
      case event: ChannelEvent.Created  => toCreate(event).widen
      case event: ChannelEvent.Updated  => toUpdate(event).widen
      case event: ChannelEvent.Deleted  => toDelete(event).widen
      case event: ChannelEvent.Restored => toRestore(event).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForChannel(value)
    }.handleErrorWith { error =>
      logger.error("Channel event processing failed", error)
      Stream.empty
    }

  def toCreate(event: ChannelEvent.Created): F[(UUID, ChannelEvent.Created)] =
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
           |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run.as(event.id.uuid -> event).transact(transactor)
    }

  def toUpdate(event: ChannelEvent.Updated): F[(UUID, ChannelEvent.Updated)] =
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
          Sync[ConnectionIO]
            .delay(logger.warn(show"Channel update ignored as it doesn't contain any modification:\n$event"))
        )(updates =>
          (fr"UPDATE channels SET" ++
            (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
            fr"WHERE id = ${event.id}").update.run.void
        )
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  def toDelete(event: ChannelEvent.Deleted): F[(UUID, ChannelEvent.Deleted)] =
    withCorrelationID(event.correlationID) {
      sql"UPDATE channels SET deleted = TRUE WHERE id = ${event.id}".update.run
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  def toRestore(event: ChannelEvent.Restored): F[(UUID, ChannelEvent.Restored)] =
    withCorrelationID(event.correlationID) {
      sql"UPDATE channels SET deleted = FALSE WHERE id = ${event.id}".update.run
        .as(event.id.uuid -> event)
        .transact(transactor)
    }
}
