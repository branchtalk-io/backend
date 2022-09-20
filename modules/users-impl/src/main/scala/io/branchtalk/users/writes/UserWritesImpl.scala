package io.branchtalk.users.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import io.branchtalk.logging.{ CorrelationID, MDC }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.model._
import io.branchtalk.users.events.{ UserCommandEvent, UsersCommandEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Session, User }
import io.scalaland.chimney.dsl._

final class UserWritesImpl[F[_]: Sync: MDC](
  producer:   EventBusProducer[F, UsersCommandEvent],
  transactor: Transactor[F]
)(implicit
  uuidGenerator: UUIDGenerator
) extends Writes[F, User, UsersCommandEvent](producer)
    with UserWrites[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val sessionExpiresInDays = 7L // TODO: make it configurable

  private val userCheck = new EntityCheck("User", transactor)

  private def reserveEmail(email: User.Email, id: Option[ID[User]] = None)(implicit pos: CodePosition): F[Unit] = {
    for {
      isReserved <- sql"""SELECT 1 FROM users WHERE email = $email AND id <> $id
                         |UNION
                         |SELECT 1 FROM reserved_emails WHERE email = $email
                         |""".stripMargin.exists
      _ <-
        if (isReserved) {
          CommonError
            .ValidationFailed(NonEmptyList.one(show"Email $email already exists"), pos)
            .raiseError[ConnectionIO, Unit]
        } else sql"""INSERT INTO reserved_emails (email) VALUES ($email)""".update.run.void
    } yield ()
  }.transact(transactor)

  private def reserveUsername(name: User.Name, id: Option[ID[User]] = None)(implicit pos: CodePosition): F[Unit] = {
    for {
      isReserved <- sql"""SELECT 1 FROM users WHERE username = $name AND id <> $id
                         |UNION
                         |SELECT 1 FROM reserved_usernames WHERE username = $name
                         |""".stripMargin.exists
      _ <-
        if (isReserved) {
          CommonError
            .ValidationFailed(NonEmptyList.one(show"Username $name already exists"), pos)
            .raiseError[ConnectionIO, Unit]
        } else sql"""INSERT INTO reserved_usernames (username) VALUES ($name)""".update.run.void
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
             |)""".stripMargin.update.run.as(algorithm -> key).transact(transactor)
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
        .withFieldConst(_.sessionExpiresAt, Session.ExpirationTime(now.offsetDateTime.plusDays(sessionExpiresInDays)))
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
          .query[(SensitiveData.Algorithm, SensitiveData.Key)]
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
      _ <- userCheck(id, sql"""SELECT 1 FROM users WHERE id = ${id}""")
      now <- ModificationTime.now[F]
      command = deletedUser
        .into[UserCommandEvent.Delete]
        .withFieldConst(_.deletedAt, now)
        .withFieldConst(_.correlationID, correlationID)
        .transform
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield DeletionScheduled(id)
}
