package io.branchtalk.users.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.events.{ UserCommandEvent, UserEvent, UsersCommandEvent, UsersEvent }
import io.scalaland.chimney.dsl._

final class UserCommandHandler[F[_]: Sync] extends Projector[F, UsersCommandEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, UsersCommandEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect { case UsersCommandEvent.ForUser(command) =>
      command
    }.evalMap[F, (UUID, UserEvent)] {
      case command: UserCommandEvent.Create.Encrypted => toCreate(command).widen
      case command: UserCommandEvent.Update.Encrypted => toUpdate(command).widen
      case command: UserCommandEvent.Delete           => toDelete(command).widen
    }.map { case (key, value) =>
      key -> UsersEvent.ForUser(value)
    }.handleErrorWith { error =>
      logger.error("User command processing failed", error)
      Stream.empty
    }

  def toCreate(command: UserCommandEvent.Create.Encrypted): F[(UUID, UserEvent.Created.Encrypted)] =
    (command.id.uuid -> command.transformInto[UserEvent.Created.Encrypted]).pure[F]

  def toUpdate(command: UserCommandEvent.Update.Encrypted): F[(UUID, UserEvent.Updated.Encrypted)] =
    (command.id.uuid -> command.transformInto[UserEvent.Updated.Encrypted]).pure[F]

  def toDelete(command: UserCommandEvent.Delete): F[(UUID, UserEvent.Deleted)] =
    (command.id.uuid -> command.transformInto[UserEvent.Deleted]).pure[F]
}
