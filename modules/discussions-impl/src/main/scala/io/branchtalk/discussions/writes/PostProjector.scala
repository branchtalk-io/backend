package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import doobie.Transactor
import fs2.Stream
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, DiscussionEvent, PostCommandEvent, PostEvent }
import io.branchtalk.discussions.infrastructure.DoobieExtensions._
import io.branchtalk.discussions.model.Post
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.scalaland.chimney.dsl._

final class PostProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, DiscussionCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionCommandEvent.ForPost(event) =>
      event
    }.evalMap[F, (UUID, PostEvent)] {
      case event: PostCommandEvent.Create  => toCreate(event).widen
      case event: PostCommandEvent.Update  => toUpdate(event).widen
      case event: PostCommandEvent.Delete  => toDelete(event).widen
      case event: PostCommandEvent.Restore => toRestore(event).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForPost(value)
    }.handleErrorWith { error =>
      logger.error("Post event processing failed", error)
      Stream.empty
    }

  def toCreate(event: PostCommandEvent.Create): F[(UUID, PostEvent.Created)] = {
    val Post.Content.Tupled(contentType, contentRaw) = event.content
    sql"""INSERT INTO posts (
         |  id,
         |  author_id,
         |  channel_id,
         |  url_title,
         |  title,
         |  content_type,
         |  content_raw,
         |  created_at
         |)
         |VALUES (
         |  ${event.id},
         |  ${event.authorID},
         |  ${event.channelID},
         |  ${event.urlTitle},
         |  ${event.title},
         |  ${contentType},
         |  ${contentRaw},
         |  ${event.createdAt}
         |)
         |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run.transact(transactor) >>
      (event.id.uuid -> event.transformInto[PostEvent.Created]).pure[F]
  }

  def toUpdate(event: PostCommandEvent.Update): F[(UUID, PostEvent.Updated)] = {
    val contentTupled = event.newContent.map(Post.Content.Tupled.unpack)
    (NonEmptyList.fromList(
      List(
        event.newUrlTitle.toUpdateFragment(fr"url_title"),
        event.newTitle.toUpdateFragment(fr"title"),
        contentTupled.map(_._1).toUpdateFragment(fr"content_type"),
        contentTupled.map(_._2).toUpdateFragment(fr"content_raw")
      ).flatten
    ) match {
      case Some(updates) =>
        (fr"UPDATE posts SET" ++
          (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
          fr"WHERE id = ${event.id}").update.run.transact(transactor).void
      case None =>
        Sync[F].delay(logger.warn(s"Post update ignored as it doesn't contain any modification:\n${event.show}"))
    }) >>
      (event.id.uuid -> event.transformInto[PostEvent.Updated]).pure[F]
  }

  def toDelete(event: PostCommandEvent.Delete): F[(UUID, PostEvent.Deleted)] =
    sql"UPDATE posts SET deleted = TRUE WHERE id = ${event.id}".update.run.transact(transactor) >>
      (event.id.uuid -> event.transformInto[PostEvent.Deleted]).pure[F]

  def toRestore(event: PostCommandEvent.Restore): F[(UUID, PostEvent.Restored)] =
    sql"UPDATE posts SET deleted = FALSE WHERE id = ${event.id}".update.run.transact(transactor) >>
      (event.id.uuid -> event.transformInto[PostEvent.Restored]).pure[F]
}
