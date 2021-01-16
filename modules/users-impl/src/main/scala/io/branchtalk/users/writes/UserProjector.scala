package io.branchtalk.users.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.branchtalk.users.events.{ UserCommandEvent, UserEvent, UsersCommandEvent, UsersEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Permission, Permissions, Session }
import io.scalaland.chimney.dsl._

final class UserProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, UsersCommandEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, UsersCommandEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect { case UsersCommandEvent.ForUser(event) =>
      event
    }.evalMap[F, (UUID, UserEvent)] {
      case event: UserCommandEvent.Create => toCreate(event).widen
      case event: UserCommandEvent.Update => toUpdate(event).widen
      case event: UserCommandEvent.Delete => toDelete(event).widen
    }.map { case (key, value) =>
      key -> UsersEvent.ForUser(value)
    }.handleErrorWith { error =>
      logger.error("User event processing failed", error)
      Stream.empty
    }

  def toCreate(event: UserCommandEvent.Create): F[(UUID, UserEvent.Created)] =
    withCorrelationID(event.correlationID) {
      {
        val Session.Usage.Tupled(sessionType, sessionPermissions) = Session.Usage.UserSession
        sql"DELETE FROM reserved_emails WHERE email = ${event.email}".update.run >>
          sql"DELETE FROM reserved_usernames WHERE username = ${event.username}".update.run >>
          sql"""INSERT INTO users (
               |  id,
               |  email,
               |  username,
               |  description,
               |  passwd_algorithm,
               |  passwd_hash,
               |  passwd_salt,
               |  permissions,
               |  created_at
               |)
               |VALUES (
               |  ${event.id},
               |  ${event.email},
               |  ${event.username},
               |  ${event.description},
               |  ${event.password.algorithm},
               |  ${event.password.hash},
               |  ${event.password.salt},
               |  ${Permissions(Set.empty)},
               |  ${event.createdAt}
               |)
               |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run >>
          sql"""INSERT INTO sessions (
               |  id,
               |  user_id,
               |  usage_type,
               |  permissions,
               |  expires_at
               |)
               |VALUES (
               |  ${event.sessionID},
               |  ${event.id},
               |  ${sessionType},
               |  ${sessionPermissions},
               |  ${event.sessionExpiresAt}
               |)""".stripMargin.update.run
      }.transact(transactor) >> (event.id.uuid -> event.transformInto[UserEvent.Created]).pure[F]
    }

  def toUpdate(event: UserCommandEvent.Update): F[(UUID, UserEvent.Updated)] = {
    import event._
    val defaultPermissions   = Permissions.empty
    val permissionsUpdateNel = NonEmptyList.fromList(updatePermissions)

    val cleanReservedIfNecessary = event.newUsername.toOption.traverse(username =>
      sql"DELETE FROM reserved_usernames WHERE username = $username".update.run
    )

    val fetchPermissionsIfNecessary = permissionsUpdateNel.fold(defaultPermissions.pure[ConnectionIO]) { _ =>
      sql"""SELECT permissions FROM users WHERE id = ${id}"""
        .query[Permissions]
        .option
        .map(_.getOrElse(defaultPermissions))
    }

    def updateUser(existingPermissions: Permissions) =
      List(
        newUsername.toUpdateFragment(fr"username"),
        newDescription.toUpdateFragment(fr"description"),
        newPassword.fold(
          p => fr"passwd_algorithm = ${p.algorithm}, passwd_hash = ${p.hash}, passwd_salt = ${p.salt}".some,
          none[Fragment]
        ),
        permissionsUpdateNel.map { nel =>
          fr"""permissions = ${nel.foldLeft(existingPermissions) {
            case (permissions, Permission.Update.Add(permission))    => permissions.append(permission)
            case (permissions, Permission.Update.Remove(permission)) => permissions.remove(permission)
          }}"""
        }
      ).flatten.pipe(NonEmptyList.fromList) match {
        case Some(updates) =>
          (fr"UPDATE users SET" ++
            (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
            fr"WHERE id = ${event.id}").update.run.void
        case None =>
          Sync[ConnectionIO].delay(
            logger.warn(show"User update ignored as it doesn't contain any modification:\n$event")
          )
      }

    withCorrelationID(event.correlationID) {
      (cleanReservedIfNecessary >> fetchPermissionsIfNecessary.flatMap(updateUser))
        .transact(transactor)
        .as(id.uuid -> event.transformInto[UserEvent.Updated])
    }
  }

  def toDelete(event: UserCommandEvent.Delete): F[(UUID, UserEvent.Deleted)] =
    withCorrelationID(event.correlationID) {
      {
        sql"DELETE FROM users WHERE id = ${event.id}".update.run >>
          sql"""INSERT INTO deleted_users (id, deleted_at)
               |VALUES (${event.id}, ${event.deletedAt})
             ON CONFLICT (id) DO NOTHING""".stripMargin.update.run
      }.transact(transactor).as(event.id.uuid -> event.transformInto[UserEvent.Deleted])
    }
}
