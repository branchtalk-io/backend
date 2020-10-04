package io.branchtalk.users.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.models.UUID
import io.branchtalk.users.events.{ UserCommandEvent, UserEvent, UsersCommandEvent, UsersEvent }
import io.scalaland.chimney.dsl._

final class UserProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, UsersCommandEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  private implicit val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, UsersCommandEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect {
        case UsersCommandEvent.ForUser(event) => event
      }
      .evalMap[F, (UUID, UserEvent)] {
        case event: UserCommandEvent.Create => toCreate(event).widen
        case event: UserCommandEvent.Update => toUpdate(event).widen
        case event: UserCommandEvent.Delete => toDelete(event).widen
      }
      .map {
        case (key, value) => key -> UsersEvent.ForUser(value)
      }
      .handleErrorWith { error =>
        logger.error("User event processing failed", error)
        Stream.empty
      }

  def toCreate(event: UserCommandEvent.Create): F[(UUID, UserEvent.Created)] = ???

  def toUpdate(event: UserCommandEvent.Update): F[(UUID, UserEvent.Updated)] = ???

  def toDelete(event: UserCommandEvent.Delete): F[(UUID, UserEvent.Deleted)] = ???
}
