package io.branchtalk.users.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.{ ID, SensitiveData, UUID }
import io.branchtalk.users.events.{ UserEvent, UsersEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions.{ *, given }
import io.branchtalk.users.model.{ Permission, Permissions, Session, User }

final class UserPostgresProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, UsersEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, UsersEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect { case UsersEvent.ForUser(event) =>
      event
    }.evalMap[F, Option[(UUID, UserEvent)]] {
      case event: UserEvent.CreatedEncrypted              => toCreate(event).widen
      case event: UserEvent.UpdatedEncrypted              => toUpdate(event).widen
      case event: UserEvent.Deleted                       => toDelete(event).widen
      case event: UserEvent.EmailUpdateRequestedEncrypted => toEmailUpdateRequested(event).widen
      case event: UserEvent.EmailConfirmed                => toEmailConfirmed(event).widen
    }.flatMap {
      case Some((key, value)) => Stream(key -> UsersEvent.ForUser(value))
      case None               => Stream.empty
    }.handleErrorWith { error =>
      logger.error("User event processing failed", error)
      Stream.empty
    }

  def toCreate(encrypted: UserEvent.CreatedEncrypted): F[Option[(UUID, UserEvent.CreatedEncrypted)]] =
    withCorrelationID(encrypted.correlationID) {
      findKeys(encrypted.id)
        .flatMap(
          _.traverse { case (algorithm, key) =>
            @SuppressWarnings(Array("org.wartremover.warts.Throw"))
            val event = encrypted.decrypt(algorithm, key).fold(e => throw new Exception(e.show), identity)

            val Session.Usage.Tupled(sessionType, sessionPermissions) = Session.Usage.UserSession

            sql"DELETE FROM reserved_emails WHERE email = ${event.email.value}"
              .updateWithLabel(show"Delete Users' Email reservation")
              .run >>
              sql"DELETE FROM reserved_usernames WHERE username = ${event.username.value}"
                .updateWithLabel(show"Delete Users' Name reservation")
                .run >>
              sql"""INSERT INTO users (
                   |  id,
                   |  email,
                   |  username,
                   |  description,
                   |  passwd_algorithm,
                   |  passwd_hash,
                   |  passwd_salt,
                   |  permissions,
                   |  email_status,
                   |  pending_email,
                   |  confirmation_token,
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
                   |  ${User.EmailStatus.New: User.EmailStatus},
                   |  ${Option.empty[User.Email]},
                   |  ${Option.empty[User.EmailConfirmationToken]},
                   |  ${event.createdAt}
                   |)
                   |ON CONFLICT (id) DO NOTHING""".stripMargin
                .updateWithLabel(show"Create Users' User ID=${event.id}")
                .run >>
              sql"""INSERT INTO sessions (
                   |  id,
                   |  user_id,
                   |  usage_type,
                   |  permissions,
                   |  expires_at,
                   |  ip_address,
                   |  user_agent
                   |)
                   |VALUES (
                   |  ${event.sessionID},
                   |  ${event.id},
                   |  ${sessionType},
                   |  ${sessionPermissions},
                   |  ${event.sessionExpiresAt},
                   |  ${Option.empty[String]},
                   |  ${Option.empty[String]}
                   |)""".stripMargin
                .updateWithLabel(show"Create Users' Session ID=${event.sessionID} for User=${event.id}")
                .run
          }
        )
        .as((encrypted.id.unwrap -> encrypted).some)
        .transact(transactor)
    }

  def toUpdate(encrypted: UserEvent.UpdatedEncrypted): F[Option[(UUID, UserEvent.UpdatedEncrypted)]] =
    withCorrelationID(encrypted.correlationID) {
      findKeys(encrypted.id)
        .flatMap(
          _.traverse { case (algorithm, key) =>
            @SuppressWarnings(Array("org.wartremover.warts.Throw"))
            val event = encrypted.decrypt(algorithm, key).fold(e => throw new Exception(e.show), identity)
            import event.*

            val defaultPermissions   = Permissions.empty
            val permissionsUpdateNel = NonEmptyList.fromList(updatePermissions)

            val cleanReservedIfNecessary = event.newUsername.toOption.traverse(username =>
              sql"DELETE FROM reserved_usernames WHERE username = ${username.value}"
                .updateWithLabel(show"Delete Users' Name=${username} reservation")
                .run
            )

            val fetchPermissionsIfNecessary = permissionsUpdateNel.fold(defaultPermissions.pure[ConnectionIO]) { _ =>
              sql"""SELECT permissions FROM users WHERE id = $id"""
                .queryWithLabel[Permissions](show"Get Users' Permissions for ID=$id")
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
                    fr"WHERE id = ${event.id}").updateWithLabel(show"Update Users' User ID=${event.id}").run.void
                case None =>
                  Sync[ConnectionIO].delay(
                    logger.warn(show"User update ignored as it doesn't contain any modification:\n$event")
                  )
              }

            (cleanReservedIfNecessary >> fetchPermissionsIfNecessary.flatMap(updateUser)).as(id.unwrap -> encrypted)
          }
        )
        .transact(transactor)
    }

  def toDelete(event: UserEvent.Deleted): F[Option[(UUID, UserEvent.Deleted)]] =
    withCorrelationID(event.correlationID) {
      {
        sql"DELETE FROM users WHERE id = ${event.id}".updateWithLabel(show"Delete Users' User ID=${event.id}").run >>
          sql"""INSERT INTO deleted_users (id, deleted_at)
               |VALUES (${event.id}, ${event.deletedAt})
             ON CONFLICT (id) DO NOTHING""".stripMargin
            .updateWithLabel(show"Record Users' deleted User ID=${event.id}")
            .run
      }.as((event.id.unwrap -> event).some).transact(transactor)
    }

  def toEmailUpdateRequested(
    encrypted: UserEvent.EmailUpdateRequestedEncrypted
  ): F[Option[(UUID, UserEvent.EmailUpdateRequestedEncrypted)]] =
    withCorrelationID(encrypted.correlationID) {
      findKeys(encrypted.id)
        .flatMap(
          _.traverse { case (algorithm, key) =>
            @SuppressWarnings(Array("org.wartremover.warts.Throw"))
            val event = encrypted.decrypt(algorithm, key).fold(e => throw new Exception(e.show), identity)

            sql"""UPDATE users SET
                 |  pending_email = ${event.newEmail.value},
                 |  confirmation_token = ${event.token},
                 |  last_modified_at = ${event.modifiedAt}
                 |WHERE id = ${event.id}""".stripMargin
              .updateWithLabel(show"Request email update for Users' User ID=${event.id}")
              .run
              .as(encrypted.id.unwrap -> encrypted)
          }
        )
        .transact(transactor)
    }

  def toEmailConfirmed(
    event: UserEvent.EmailConfirmed
  ): F[Option[(UUID, UserEvent.EmailConfirmed)]] =
    withCorrelationID(event.correlationID) {
      // Fetch the pending email and current token
      sql"""SELECT pending_email, confirmation_token FROM users WHERE id = ${event.id}"""
        .queryWithLabel[(Option[User.Email], Option[User.EmailConfirmationToken])](
          show"Get Users' pending email for ID=${event.id}"
        )
        .option
        .flatMap {
          case Some((Some(pendingEmail), Some(storedToken))) if storedToken === event.token =>
            sql"""UPDATE users SET
                 |  email = $pendingEmail,
                 |  email_status = ${User.EmailStatus.Confirmed: User.EmailStatus},
                 |  pending_email = ${Option.empty[User.Email]},
                 |  confirmation_token = ${Option.empty[User.EmailConfirmationToken]},
                 |  last_modified_at = ${event.modifiedAt}
                 |WHERE id = ${event.id}""".stripMargin
              .updateWithLabel(show"Confirm email for Users' User ID=${event.id}")
              .run
              .void
          case _ =>
            Sync[ConnectionIO].delay(
              logger.warn(show"Email confirmation ignored for User ID=${event.id}: token mismatch or no pending email")
            )
        }.as((event.id.unwrap -> event).some).transact(transactor)
    }

  private def findKeys(userID: ID[User]): ConnectionIO[Option[(SensitiveData.Algorithm, SensitiveData.Key)]] =
    sql"""SELECT enc_algorithm, key_value FROM sensitive_data_keys WHERE user_id = $userID"""
      .queryWithLabel[(SensitiveData.Algorithm, SensitiveData.Key)](
        show"Get encryption keys for Users' User ID=$userID"
      )
      .option
}
