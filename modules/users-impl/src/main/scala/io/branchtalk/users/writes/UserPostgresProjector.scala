package io.branchtalk.users.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.{ ID, SensitiveData, UUID }
import io.branchtalk.users.events.{ UserEvent, UsersEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Permission, Permissions, Session, User }

final class UserPostgresProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, UsersEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, UsersEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect { case UsersEvent.ForUser(event) =>
      event
    }.evalMap[F, Option[(UUID, UserEvent)]] {
      case event: UserEvent.Created.Encrypted => toCreate(event).widen
      case event: UserEvent.Updated.Encrypted => toUpdate(event).widen
      case event: UserEvent.Deleted           => toDelete(event).widen
    }.flatMap {
      case Some((key, value)) => Stream(key -> UsersEvent.ForUser(value))
      case None               => Stream.empty
    }.handleErrorWith { error =>
      logger.error("User event processing failed", error)
      Stream.empty
    }

  // scalastyle:off method.length
  def toCreate(encrypted: UserEvent.Created.Encrypted): F[Option[(UUID, UserEvent.Created.Encrypted)]] =
    withCorrelationID(encrypted.correlationID) {
      findKeys(encrypted.id)
        .flatMap(
          _.traverse { case (algorithm, key) =>
            @SuppressWarnings(Array("org.wartremover.warts.Throw"))
            val event = encrypted.decrypt(algorithm, key).fold(e => throw new Exception(e.show), identity)

            val Session.Usage.Tupled(sessionType, sessionPermissions) = Session.Usage.UserSession

            sql"DELETE FROM reserved_emails WHERE email = ${event.email.value}".update.run >>
              sql"DELETE FROM reserved_usernames WHERE username = ${event.username.value}".update.run >>
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
                   |  ${event.email.value},
                   |  ${event.username.value},
                   |  ${event.description},
                   |  ${event.password.value.algorithm},
                   |  ${event.password.value.hash},
                   |  ${event.password.value.salt},
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
          }
        )
        .as((encrypted.id.uuid -> encrypted).some)
        .transact(transactor)
    }
  // scalastyle:on method.length

  // scalastyle:off method.length
  def toUpdate(encrypted: UserEvent.Updated.Encrypted): F[Option[(UUID, UserEvent.Updated.Encrypted)]] =
    withCorrelationID(encrypted.correlationID) {
      findKeys(encrypted.id)
        .flatMap(
          _.traverse { case (algorithm, key) =>
            @SuppressWarnings(Array("org.wartremover.warts.Throw"))
            val event = encrypted.decrypt(algorithm, key).fold(e => throw new Exception(e.show), identity)
            import event._

            val defaultPermissions   = Permissions.empty
            val permissionsUpdateNel = NonEmptyList.fromList(updatePermissions)

            val cleanReservedIfNecessary = event.newUsername.toOption
              .traverse(username => sql"DELETE FROM reserved_usernames WHERE username = ${username.value}".update.run)

            val fetchPermissionsIfNecessary = permissionsUpdateNel.fold(defaultPermissions.pure[ConnectionIO]) { _ =>
              sql"""SELECT permissions FROM users WHERE id = $id"""
                .query[Permissions]
                .option
                .map(_.getOrElse(defaultPermissions))
            }

            def updateUser(existingPermissions: Permissions) =
              List(
                newUsername.map(_.value).toUpdateFragment(fr"username"),
                newDescription.toUpdateFragment(fr"description"),
                newPassword
                  .map(_.value)
                  .fold(
                    p => fr"""passwd_algorithm = ${p.algorithm},
                             |passwd_hash = ${p.hash},
                             |passwd_salt = ${p.salt}""".stripMargin.some,
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

            (cleanReservedIfNecessary >> fetchPermissionsIfNecessary.flatMap(updateUser)).as(id.uuid -> encrypted)
          }
        )
        .transact(transactor)
    }
  // scalastyle:on method.length

  def toDelete(event: UserEvent.Deleted): F[Option[(UUID, UserEvent.Deleted)]] =
    withCorrelationID(event.correlationID) {
      {
        sql"DELETE FROM users WHERE id = ${event.id}".update.run >>
          sql"""INSERT INTO deleted_users (id, deleted_at)
               |VALUES (${event.id}, ${event.deletedAt})
             ON CONFLICT (id) DO NOTHING""".stripMargin.update.run
      }.as((event.id.uuid -> event).some).transact(transactor)
    }

  private def findKeys(userID: ID[User]): ConnectionIO[Option[(SensitiveData.Algorithm, SensitiveData.Key)]] =
    sql"""SELECT enc_algorithm, key_value FROM sensitive_data_keys WHERE user_id = $userID"""
      .query[(SensitiveData.Algorithm, SensitiveData.Key)]
      .option
}
