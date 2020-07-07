package io.subfibers.discussions.writes

import cats.effect.Sync
import cats.implicits._
import doobie.Transactor
import fs2.Stream
import io.scalaland.chimney.dsl._
import io.subfibers.discussions.events.{ PostCommandEvent, PostEvent }
import io.subfibers.shared.infrastructure.Projector

class PostProjector[F[_]: Sync](transactor: Transactor[F]) extends Projector[F, PostCommandEvent, PostEvent] {

  override def apply(in: Stream[F, PostCommandEvent]): Stream[F, PostEvent] = in.evalMap[F, PostEvent] {
    case event: PostCommandEvent.Create => toCreate(event).widen
    case event: PostCommandEvent.Update => toUpdate(event).widen
    case event: PostCommandEvent.Delete => toDelete(event).widen
  }

  def toCreate(event: PostCommandEvent.Create): F[PostEvent.Created] = {
    val response = event.transformInto[PostEvent.Created]
    // TODO: update db
    response.pure[F]
  }
  def toUpdate(event: PostCommandEvent.Update): F[PostEvent.Updated] = {
    val response = event.transformInto[PostEvent.Updated]
    // TODO: update db
    response.pure[F]
  }
  def toDelete(event: PostCommandEvent.Delete): F[PostEvent.Deleted] = {
    val response = event.transformInto[PostEvent.Deleted]
    // TODO: update db
    response.pure[F]
  }
}
