package io.branchtalk.users.writes

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.events.{ UserCommandEvent, UserEvent, UsersCommandEvent, UsersEvent }
import io.scalaland.chimney.dsl.*

final class UserCommandHandler[F[_]: Sync] extends Projector[F, UsersCommandEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, UsersCommandEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect { case UsersCommandEvent.ForUser(command) =>
      command
    }.evalMap[F, (UUID, UserEvent)] {
      case command: UserCommandEvent.CreateEncrypted              => toCreate(command).widen
      case command: UserCommandEvent.UpdateEncrypted              => toUpdate(command).widen
      case command: UserCommandEvent.Delete                       => toDelete(command).widen
      case command: UserCommandEvent.RequestEmailUpdateEncrypted  => toRequestEmailUpdate(command).widen
      case command: UserCommandEvent.ConfirmEmail                 => toConfirmEmail(command).widen
    }.map { case (key, value) =>
      key -> UsersEvent.ForUser(value)
    }.handleErrorWith { error =>
      logger.error("User command processing failed", error)
      Stream.empty
    }

  def toCreate(command: UserCommandEvent.CreateEncrypted): F[(UUID, UserEvent.CreatedEncrypted)] =
    (command.id.unwrap -> command.transformInto[UserEvent.CreatedEncrypted]).pure[F]

  def toUpdate(command: UserCommandEvent.UpdateEncrypted): F[(UUID, UserEvent.UpdatedEncrypted)] =
    (command.id.unwrap -> command.transformInto[UserEvent.UpdatedEncrypted]).pure[F]

  def toDelete(command: UserCommandEvent.Delete): F[(UUID, UserEvent.Deleted)] =
    (command.id.unwrap -> command.transformInto[UserEvent.Deleted]).pure[F]

  def toRequestEmailUpdate(
    command: UserCommandEvent.RequestEmailUpdateEncrypted
  ): F[(UUID, UserEvent.EmailUpdateRequestedEncrypted)] =
    (command.id.unwrap -> command.transformInto[UserEvent.EmailUpdateRequestedEncrypted]).pure[F]

  def toConfirmEmail(command: UserCommandEvent.ConfirmEmail): F[(UUID, UserEvent.EmailConfirmed)] =
    (command.id.unwrap -> command.transformInto[UserEvent.EmailConfirmed]).pure[F]
}
