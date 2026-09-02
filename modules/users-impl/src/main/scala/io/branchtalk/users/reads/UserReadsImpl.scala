package io.branchtalk.users.reads

import cats.effect.Sync
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.*
import io.branchtalk.users.infrastructure.DoobieExtensions.{ *, given }
import io.branchtalk.users.model.*

final class UserReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends UserReads[F] {

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       email,
        |       username,
        |       description,
        |       passwd_algorithm,
        |       passwd_hash,
        |       passwd_salt,
        |       permissions,
        |       email_status,
        |       pending_email,
        |       confirmation_token,
        |       created_at,
        |       last_modified_at
        |FROM users""".stripMargin

  private val filtered: User.Filter => Fragment = {
    case User.Filter.HasPermission(permission)   => fr"permissions @> jsonb_build_array($permission)"
    case User.Filter.HasPermissions(permissions) => fr"permissions @> $permissions"
  }

  private val orderBy: User.Sorting => Fragment = {
    case User.Sorting.Newest              => fr"ORDER BY created_at DESC"
    case User.Sorting.NameAlphabetically  => fr"ORDER BY username ASC"
    case User.Sorting.EmailAlphabetically => fr"ORDER BY email ASC"
  }

  private def idExists(id: ID[User]): Fragment = fr"id = $id"

  override def authenticate(username: User.Name, password: Password.Raw): F[User] =
    (commonSelect ++ fr"WHERE username = ${username}")
      .queryWithLabel[UserDao](show"Authenticate Users' User for Name=${username}")
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
    sortBy:  User.Sorting,
    offset:  Paginated.Offset,
    limit:   Paginated.Limit,
    filters: List[User.Filter] = List.empty
  ): F[Paginated[User]] =
    (commonSelect ++ Fragments.whereAndOpt(filters.map(filtered)) ++ orderBy(sortBy))
      .paginate[UserDao](offset, limit, show"Paginate Users' Session from $offset taking $limit sorted by $sortBy")
      .map(_.map(_.toDomain))
      .transact(transactor)

  override def exists(id: ID[User]): F[Boolean] =
    (fr"SELECT 1 FROM users WHERE" ++ idExists(id)).exists(show"Users' User ID=$id exists").transact(transactor)

  override def deleted(id: ID[User]): F[Boolean] =
    (fr"SELECT 1 FROM deleted_users WHERE" ++ idExists(id))
      .exists(show"Users' User ID=$id deleted")
      .transact(transactor)

  override def getById(id: ID[User]): F[Option[User]] =
    (commonSelect ++ fr"WHERE" ++ idExists(id))
      .queryWithLabel[UserDao](show"Get Users' User by ID=$id")
      .map(_.toDomain)
      .option
      .transact(transactor)

  override def requireById(id: ID[User]): F[User] =
    (commonSelect ++ fr"WHERE" ++ idExists(id))
      .queryWithLabel[UserDao](show"Require Users' User by ID=$id")
      .map(_.toDomain)
      .failNotFound("User", id)
      .transact(transactor)
}
