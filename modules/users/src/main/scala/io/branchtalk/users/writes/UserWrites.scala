package io.branchtalk.users.writes

import io.branchtalk.shared.model.{ CreationScheduled, DeletionScheduled, UpdateScheduled }
import io.branchtalk.users.model.{ Session, User }

trait UserWrites[F[_]] {

  def createUser(newUser:     User.Create): F[(CreationScheduled[User], CreationScheduled[Session])]
  def updateUser(updatedUser: User.Update): F[UpdateScheduled[User]]
  def deleteUser(deletedUser: User.Delete): F[DeletionScheduled[User]]
}
