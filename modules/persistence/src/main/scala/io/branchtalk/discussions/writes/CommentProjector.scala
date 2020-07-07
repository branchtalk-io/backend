package io.branchtalk.discussions.writes

import cats.effect.Sync
import cats.implicits._
import doobie.Transactor
import fs2.Stream
import io.scalaland.chimney.dsl._
import io.branchtalk.discussions.events.{ CommentCommandEvent, CommentEvent }
import io.branchtalk.shared.infrastructure.Projector

class CommentProjector[F[_]: Sync](transactor: Transactor[F]) extends Projector[F, CommentCommandEvent, CommentEvent] {

  override def apply(in: Stream[F, CommentCommandEvent]): Stream[F, CommentEvent] = in.evalMap[F, CommentEvent] {
    case event: CommentCommandEvent.Create => toCreate(event).widen
    case event: CommentCommandEvent.Update => toUpdate(event).widen
    case event: CommentCommandEvent.Delete => toDelete(event).widen
  }

  def toCreate(event: CommentCommandEvent.Create): F[CommentEvent.Created] = {
    val response = event.transformInto[CommentEvent.Created]
    // TODO: update db
    response.pure[F]
  }
  def toUpdate(event: CommentCommandEvent.Update): F[CommentEvent.Updated] = {
    val response = event.transformInto[CommentEvent.Updated]
    // TODO: update db
    response.pure[F]
  }
  def toDelete(event: CommentCommandEvent.Delete): F[CommentEvent.Deleted] = {
    val response = event.transformInto[CommentEvent.Deleted]
    // TODO: update db
    response.pure[F]
  }
}
