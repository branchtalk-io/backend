package io.branchtalk.users.writes

import io.branchtalk.shared.models._
import io.branchtalk.users.model.User

final class UserWritesImpl[F[_]] extends UserWrites[F] {

  override def createUser(newUser: User.Create): F[CreationScheduled[User]] = ???

  override def updateUser(updatedUser: User.Update): F[UpdateScheduled[User]] = ???

  override def deleteUser(deletedUser: User.Delete): F[DeletionScheduled[User]] = ???
}
