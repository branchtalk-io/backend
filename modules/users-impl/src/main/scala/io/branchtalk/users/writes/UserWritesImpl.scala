package io.branchtalk.users.writes

import cats.effect.{ Sync, Timer }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.{ EventBusProducer, Writes }
import io.branchtalk.shared.models._
import io.branchtalk.users.events.{ UserCommandEvent, UsersCommandEvent }
import io.branchtalk.users.model.User
import io.scalaland.chimney.dsl._

final class UserWritesImpl[F[_]: Sync: Timer](
  producer:   EventBusProducer[F, UsersCommandEvent],
  transactor: Transactor[F]
)(
  implicit uuidGenerator: UUIDGenerator
) extends Writes[F, User, UsersCommandEvent](producer)
    with UserWrites[F] {

  private val userCheck = new EntityCheck("User", transactor)

  override def createUser(newUser: User.Create): F[CreationScheduled[User]] =
    for {
      id <- UUID.create[F].map(ID[User])
      now <- CreationTime.now[F]
      command = newUser
        .into[UserCommandEvent.Create]
        .withFieldConst(_.id, id)
        .withFieldConst(_.createdAt, now)
        .transform
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield CreationScheduled(id)

  override def updateUser(updatedUser: User.Update): F[UpdateScheduled[User]] =
    for {
      id <- updatedUser.id.pure[F]
      _ <- userCheck(id, sql"""SELECT 1 FROM users WHERE id = ${id}""")
      now <- ModificationTime.now[F]
      command = updatedUser.into[UserCommandEvent.Update].withFieldConst(_.modifiedAt, now).transform
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield UpdateScheduled(id)

  override def deleteUser(deletedUser: User.Delete): F[DeletionScheduled[User]] =
    for {
      id <- deletedUser.id.pure[F]
      _ <- userCheck(id, sql"""SELECT 1 FROM deleted_users WHERE id = ${id}""")
      command = deletedUser.into[UserCommandEvent.Delete].transform
      _ <- postEvent(id, UsersCommandEvent.ForUser(command))
    } yield DeletionScheduled(id)
}
