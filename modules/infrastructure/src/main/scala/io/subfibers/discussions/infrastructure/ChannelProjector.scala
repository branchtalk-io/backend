package io.subfibers.discussions.infrastructure

import cats.effect.Sync
import cats.implicits._
import doobie.Transactor
import fs2.Stream
import io.scalaland.chimney.dsl._
import io.subfibers.discussions.events.{ ChannelCommandEvent, ChannelEvent }
import io.subfibers.shared.infrastructure.Projector

class ChannelProjector[F[_]: Sync](transactor: Transactor[F]) extends Projector[F, ChannelCommandEvent, ChannelEvent] {

  override def apply(in: Stream[F, ChannelCommandEvent]): Stream[F, ChannelEvent] = in.evalMap[F, ChannelEvent] {
    case event: ChannelCommandEvent.Create => toCreate(event).widen
    case event: ChannelCommandEvent.Update => toUpdate(event).widen
    case event: ChannelCommandEvent.Delete => toDelete(event).widen
  }

  def toCreate(event: ChannelCommandEvent.Create): F[ChannelEvent.Created] = {
    val response = event.transformInto[ChannelEvent.Created]
    // TODO: update db
    response.pure[F]
  }
  def toUpdate(event: ChannelCommandEvent.Update): F[ChannelEvent.Updated] = {
    val response = event.transformInto[ChannelEvent.Updated]
    // TODO: update db
    response.pure[F]
  }
  def toDelete(event: ChannelCommandEvent.Delete): F[ChannelEvent.Deleted] = {
    val response = event.transformInto[ChannelEvent.Deleted]
    // TODO: update db
    response.pure[F]
  }
}
