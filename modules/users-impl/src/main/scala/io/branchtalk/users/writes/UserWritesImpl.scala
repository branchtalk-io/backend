package io.branchtalk.users.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.*
import io.branchtalk.users.events.{ UserCommandEvent, UsersCommandEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions.{ *, given }
import io.branchtalk.users.model.{ Password, Session, User }
import io.scalaland.chimney.dsl.*

final class UserWritesImpl[F[_]: Sync: MDC](
  producer:             KafkaEventBus.Producer[F, UsersCommandEvent],
  transactor:           Transactor[F],
  passwordConfig:       Password.Config = Password.Config(),
  sessionExpiresInDays: Long = 7L
)(using UUID.Generator)
    extends Writes[F, User, UsersCommandEvent](producer),
      UserWrites[F] {

  private val userCheck = new EntityCheck("User", transactor)

  private def reserveEmail(email: User.Email, id: Option[ID[User]] = None)(using CodePosition): F[Unit] = {
    for {
      isReserved <- sql"""SELECT 1 FROM users WHERE email = $email AND id <> $id
                         |UNION
                         |SELECT 1 FROM reserved_emails WHERE email = $email
                         |""".stripMargin.exists(show"Check if Users' Email=$email is reserved for ID=$id")
      _ <-
        if (isReserved) {
          CommonError.validationFailed(show"Email $email already exists").raiseError[ConnectionIO, Unit]
        } else
          sql"""INSERT INTO reserved_emails (email) VALUES ($email)"""
            .updateWithLabel(show"Reserve Users' Email=$email for ID=$id")
            .run
            .void
    } yield ()
  }.transact(transactor)

  private def reserveUsername(name: User.Name, id: Option[ID[User]] = None)(using CodePosition): F[Unit] = {
    for {
      isReserved <- sql"""SELECT 1 FROM users WHERE username = $name AND id <> $id
                         |UNION
                         |SELECT 1 FROM reserved_usernames WHERE username = $name
                         |""".stripMargin.exists(show"Check if Users' Name=$name is reserved for ID=$id")
      _ <-
        if (isReserved) {
          CommonError.validationFailed(show"Username $name already exists").raiseError[ConnectionIO, Unit]
        } else
          sql"""INSERT INTO reserved_usernames (username) VALUES ($name)"""
            .updateWithLabel(show"Reserve Users' Name=$name for ID=$id")
            .run
            .void
    } yield ()
  }.transact(transactor)

  override def createUser(newUser: User.Create): F[(CreationScheduled[User], CreationScheduled[Session])] =
    for {
      _ <- reserveEmail(newUser.email)
      _ <- reserveUsername(newUser.username)
      id <- ID.create[F, User]
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      (algorithm, key) <- {
        val algorithm = SensitiveData.Algorithm.default
        val key       = algorithm.generateKey()
        sql"""INSERT INTO sensitive_data_keys (
             |  user_id,
             |  key_value,
             |  enc_algorithm
             |) VALUES (
             |  $id,
             |  $key,
             |  $algorithm
             |)""".stripMargin
          .updateWithLabel(show"Create Users' User ID=$id")
          .run
          .as(algorithm -> key)
          .transact(transactor)
      }
      sessionID <- ID.create[F, Session]
      now <- CreationTime.now[F]
      command = newUser
        .into[UserCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldComputed(_.email, _.email.pipe(SensitiveData(_)))
        .withFieldComputed(_.username, _.username.pipe(SensitiveData(_)))
        .withFieldComputed(_.password, _.password.pipe(SensitiveData(_)))
        .withFieldConst(_.createdAt, now)
        .withFieldConst(_.sessionID, sessionID)
        .withFieldConst(_.sessionExpiresAt, Session.ExpirationTime(now.unwrap.plusDays(sessionExpiresInDays)))
        .withFieldConst(_.correlationID, correlationID)
        .transform
        .encrypt(algorithm, key)
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield (CreationScheduled(id), CreationScheduled(sessionID))

  override def updateUser(updatedUser: User.Update): F[UpdateScheduled[User]] =
    for {
      _ <- updatedUser.newUsername.toOption.traverse(reserveUsername(_, updatedUser.id.some))
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = updatedUser.id
      _ <- userCheck(id, sql"""SELECT 1 FROM users WHERE id = $id""")
      (algorithm, key) <-
        sql"""SELECT enc_algorithm, key_value FROM sensitive_data_keys WHERE user_id = $id"""
          .queryWithLabel[(SensitiveData.Algorithm, SensitiveData.Key)](
            show"Get encryption keys for Users' User ID=$id"
          )
          .unique
          .transact(transactor)
      now <- ModificationTime.now[F]
      command = updatedUser
        .into[UserCommandEvent.Update]
        .withFieldComputed(_.newUsername, _.newUsername.map(SensitiveData(_)))
        .withFieldComputed(_.newPassword, _.newPassword.map(SensitiveData(_)))
        .withFieldConst(_.modifiedAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
        .encrypt(algorithm, key)
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield UpdateScheduled(id)

  override def deleteUser(deletedUser: User.Delete): F[DeletionScheduled[User]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = deletedUser.id
      _ <- userCheck(id, sql"""SELECT 1 FROM users WHERE id = $id""")
      now <- ModificationTime.now[F]
      command = deletedUser
        .into[UserCommandEvent.Delete]
        .withFieldConst(_.deletedAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield DeletionScheduled(id)

  override def requestEmailUpdate(
    request: User.RequestEmailUpdate
  ): F[(UpdateScheduled[User], User.EmailConfirmationToken)] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = request.id
      _ <- userCheck(id, sql"""SELECT 1 FROM users WHERE id = $id""")
      (algorithm, key) <-
        sql"""SELECT enc_algorithm, key_value FROM sensitive_data_keys WHERE user_id = $id"""
          .queryWithLabel[(SensitiveData.Algorithm, SensitiveData.Key)](
            show"Get encryption keys for Users' User ID=$id"
          )
          .unique
          .transact(transactor)
      now <- ModificationTime.now[F]
      token <- Sync[F].delay(java.util.UUID.randomUUID().toString)
        .flatMap(ParseNewtype[F].parse[User.EmailConfirmationToken](_))
      command = UserCommandEvent.RequestEmailUpdate(
        id = id,
        newEmail = SensitiveData(request.newEmail),
        token = token,
        modifiedAt = now,
        correlationID = correlationID
      ).encrypt(algorithm, key)
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield (UpdateScheduled(id), token)

  override def confirmEmail(confirm: User.ConfirmEmail): F[UpdateScheduled[User]] =
    for {
      correlationID <- CorrelationID.getCurrentOrGenerate[F]
      id = confirm.id
      _ <- userCheck(id, sql"""SELECT 1 FROM users WHERE id = $id""")
      now <- ModificationTime.now[F]
      command = UserCommandEvent.ConfirmEmail(
        id = id,
        token = confirm.token,
        modifiedAt = now,
        correlationID = correlationID
      )
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield UpdateScheduled(id)
}
