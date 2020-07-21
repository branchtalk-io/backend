package io.branchtalk.discussions.writes

import cats.effect.Sync
import doobie.Transactor
import fs2.Stream
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, DiscussionEvent, PostCommandEvent, PostEvent }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.models.UUID

final class PostProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, DiscussionCommandEvent, (UUID, DiscussionEvent)] {

  override def apply(in: Stream[F, DiscussionCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect {
        case DiscussionCommandEvent.ForPost(event) => event
      }
      .evalMap[F, (UUID, PostEvent)] {
        case event: PostCommandEvent.Create => toCreate(event).widen
        case event: PostCommandEvent.Update => toUpdate(event).widen
        case event: PostCommandEvent.Delete => toDelete(event).widen
      }
      .map {
        case (key, value) => key -> DiscussionEvent.ForPost(value)
      }

  def toCreate(event: PostCommandEvent.Create): F[(UUID, PostEvent.Created)] = {
    val response = event.transformInto[PostEvent.Created]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
  def toUpdate(event: PostCommandEvent.Update): F[(UUID, PostEvent.Updated)] = {
    val response = event.transformInto[PostEvent.Updated]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
  def toDelete(event: PostCommandEvent.Delete): F[(UUID, PostEvent.Deleted)] = {
    val response = event.transformInto[PostEvent.Deleted]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
}
