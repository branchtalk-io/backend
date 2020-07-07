package io.branchtalk.discussions.writes

import cats.effect.Sync
import cats.implicits._
import doobie.Transactor
import fs2.Stream
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ ChannelCommandEvent, ChannelEvent, DiscussionCommandEvent, DiscussionEvent }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.models.UUID

final class ChannelProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, DiscussionCommandEvent, (UUID, DiscussionEvent)] {

  override def apply(in: Stream[F, DiscussionCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect {
        case DiscussionCommandEvent.ForChannel(event) => event
      }
      .evalMap[F, (UUID, ChannelEvent)] {
        case event: ChannelCommandEvent.Create => toCreate(event).widen
        case event: ChannelCommandEvent.Update => toUpdate(event).widen
        case event: ChannelCommandEvent.Delete => toDelete(event).widen
      }
      .map {
        case (key, value) => key -> DiscussionEvent.ForChannel(value)
      }

  def toCreate(event: ChannelCommandEvent.Create): F[(UUID, ChannelEvent.Created)] = {
    val response = event.transformInto[ChannelEvent.Created]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
  def toUpdate(event: ChannelCommandEvent.Update): F[(UUID, ChannelEvent.Updated)] = {
    val response = event.transformInto[ChannelEvent.Updated]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
  def toDelete(event: ChannelCommandEvent.Delete): F[(UUID, ChannelEvent.Deleted)] = {
    val response = event.transformInto[ChannelEvent.Deleted]
    // TODO: update db
    (response.id.value -> response).pure[F]
  }
}
