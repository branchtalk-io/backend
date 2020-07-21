package io.branchtalk.discussions.writes

import cats.effect.Sync
import doobie.Transactor
import fs2.Stream
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ CommentCommandEvent, CommentEvent, DiscussionCommandEvent, DiscussionEvent }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.models.UUID

final class CommentProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, DiscussionCommandEvent, (UUID, DiscussionEvent)] {

  override def apply(in: Stream[F, DiscussionCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect {
        case DiscussionCommandEvent.ForComment(event) => event
      }
      .evalMap[F, (UUID, CommentEvent)] {
        case event: CommentCommandEvent.Create => toCreate(event).widen
        case event: CommentCommandEvent.Update => toUpdate(event).widen
        case event: CommentCommandEvent.Delete => toDelete(event).widen
      }
      .map {
        case (key, value) => key -> DiscussionEvent.ForComment(value)
      }

  def toCreate(event: CommentCommandEvent.Create): F[(UUID, CommentEvent.Created)] = {
    val response = event.transformInto[CommentEvent.Created]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
  def toUpdate(event: CommentCommandEvent.Update): F[(UUID, CommentEvent.Updated)] = {
    val response = event.transformInto[CommentEvent.Updated]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
  def toDelete(event: CommentCommandEvent.Delete): F[(UUID, CommentEvent.Deleted)] = {
    val response = event.transformInto[CommentEvent.Deleted]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
}
