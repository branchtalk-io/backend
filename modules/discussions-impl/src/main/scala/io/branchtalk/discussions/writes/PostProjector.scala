package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.Transactor
import fs2.Stream
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, DiscussionEvent, PostCommandEvent, PostEvent }
import io.branchtalk.discussions.model.Post
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.models.UUID

final class PostProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, DiscussionCommandEvent, (UUID, DiscussionEvent)] {

  override def apply(in: Stream[F, DiscussionCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect {
        case DiscussionCommandEvent.ForPost(event) => event
      }
      .evalMap[F, (UUID, PostEvent)] {
        case event: PostCommandEvent.Create  => toCreate(event).widen
        case event: PostCommandEvent.Update  => toUpdate(event).widen
        case event: PostCommandEvent.Delete  => toDelete(event).widen
        case event: PostCommandEvent.Restore => toRestore(event).widen
      }
      .map {
        case (key, value) => key -> DiscussionEvent.ForPost(value)
      }

  def toCreate(event: PostCommandEvent.Create): F[(UUID, PostEvent.Created)] = {
    val Post.Content.Tupled(contentType, contentRaw) = event.content
    sql"""
      INSERT INTO posts (
        id,
        author_id,
        url_title,
        title,
        content_type,
        content_raw,
        created_at
      )
      VALUE (
        ${event.id},
        ${event.authorID},
        ${event.urlTitle},
        ${event.title},
        ${contentType},
        ${contentRaw},
        ${event.createdAt}
      )
      ON CONFLICT DO NOTHING
    """.update.run.transact(transactor) >>
      (event.id.value -> event.transformInto[PostEvent.Created]).pure[F]
  }

  def toUpdate(event: PostCommandEvent.Update): F[(UUID, PostEvent.Updated)] = {
    val contentTupled = event.newContent.map(Post.Content.Tupled.unpack)
    (NonEmptyList
      .of(
        event.newUrlTitle.toUpdateFragment(fr"url_title"),
        event.newTitle.toUpdateFragment(fr"title"),
        contentTupled.map(_._1).toUpdateFragment(fr"content_type"),
        contentTupled.map(_._2).toUpdateFragment(fr"content_raw")
      )
      .sequence match {
      case Some(updates) =>
        (fr"UPDATE posts SET" ++
          updates.intercalate(fr",") ++
          fr", last_updated_at = ${event.modifiedAt} WHERE id = ${event.id}").update.run.transact(transactor).void
      case None =>
        ().pure[F]
    }) >>
      (event.id.value -> event.transformInto[PostEvent.Updated]).pure[F]
  }

  def toDelete(event: PostCommandEvent.Delete): F[(UUID, PostEvent.Deleted)] =
    sql"UPDATE posts SET deleted = TRUE WHERE id = ${event.id}".update.run.transact(transactor) >>
      (event.id.value -> event.transformInto[PostEvent.Deleted]).pure[F]

  def toRestore(event: PostCommandEvent.Restore): F[(UUID, PostEvent.Restored)] =
    sql"UPDATE posts SET deleted = FALSE WHERE id = ${event.id}".update.run.transact(transactor) >>
      (event.id.value -> event.transformInto[PostEvent.Restored]).pure[F]
}
