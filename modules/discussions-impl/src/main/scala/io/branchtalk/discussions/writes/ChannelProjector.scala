package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.discussions.events.{ ChannelCommandEvent, ChannelEvent, DiscussionCommandEvent, DiscussionEvent }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.models.UUID
import io.scalaland.chimney.dsl._

final class ChannelProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, DiscussionCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionCommandEvent.ForChannel(event) =>
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
         |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run.transact(transactor) >>
      (event.id.uuid -> event.transformInto[ChannelEvent.Created]).pure[F]

  def toUpdate(event: ChannelCommandEvent.Update): F[(UUID, ChannelEvent.Updated)] =
    (NonEmptyList.fromList(
      List(
        event.newUrlName.toUpdateFragment(fr"url_name"),
        event.newName.toUpdateFragment(fr"name"),
        event.newDescription.toUpdateFragment(fr"description")
      ).flatten
    ) match {
      case Some(updates) =>
        (fr"UPDATE channels SET" ++
          (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
          fr"WHERE id = ${event.id}").update.run.transact(transactor).void
      case None =>
        Sync[F].delay(logger.warn(s"Channel update ignored as it doesn't contain any modification:\n${event.show}"))
    }) >>
      (event.id.uuid -> event.transformInto[ChannelEvent.Updated]).pure[F]

  def toDelete(event: ChannelCommandEvent.Delete): F[(UUID, ChannelEvent.Deleted)] =
    sql"UPDATE channels SET deleted = TRUE WHERE id = ${event.id}".update.run.transact(transactor) >>
      (event.id.uuid -> event.transformInto[ChannelEvent.Deleted]).pure[F]

  def toRestore(event: ChannelCommandEvent.Restore): F[(UUID, ChannelEvent.Restored)] =
    sql"UPDATE channels SET deleted = FALSE WHERE id = ${event.id}".update.run.transact(transactor) >>
      (event.id.uuid -> event.transformInto[ChannelEvent.Restored]).pure[F]
}
