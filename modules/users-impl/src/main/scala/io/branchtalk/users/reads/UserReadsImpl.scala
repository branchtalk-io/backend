package io.branchtalk.users.reads

import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.Paginated
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Password, User, UserDao }

final class UserReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends UserReads[F] {

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       email,
        |       username,
        |       description,
        |       passwd_algorithm,
        |       passwd_hash,
        |       passwd_salt,
        |       permissions,
        |       created_at,
        |       last_modified_at
        |FROM users""".stripMargin

  private def idExists(id: ID[User]): Fragment = fr"id = ${id}"

  override def authenticate(username: User.Name, password: Password.Raw): F[User] =
    (commonSelect ++ fr"WHERE username = ${username}")
      .query[UserDao]
      .map(_.toDomain)
      .option
      .transact(transactor)
      .flatMap {
        case Some(user) if user.data.password.verify(password) =>
          user.pure[F]
        case _ =>
          (CommonError.InvalidCredentials(CodePosition.providePosition): CommonError).raiseError[F, User]
      }

  override def paginate(
    offset: Long Refined NonNegative,
    limit:  Int Refined Positive
  ): F[Paginated[User]] =
    commonSelect.paginate[UserDao](offset, limit).map(_.map(_.toDomain)).transact(transactor)

  override def exists(id: ID[User]): F[Boolean] =
    (fr"SELECT 1 FROM users WHERE" ++ idExists(id)).exists.transact(transactor)

  override def deleted(id: ID[User]): F[Boolean] =
    (fr"SELECT 1 FROM deleted_users WHERE" ++ idExists(id)).exists.transact(transactor)

  override def getById(id: ID[User]): F[Option[User]] =
    (commonSelect ++ fr"WHERE" ++ idExists(id)).query[UserDao].map(_.toDomain).option.transact(transactor)

  override def requireById(id: ID[User]): F[User] =
    (commonSelect ++ fr"WHERE" ++ idExists(id))
      .query[UserDao]
      .map(_.toDomain)
      .failNotFound("User", id)
      .transact(transactor)
}
