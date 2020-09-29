package io.branchtalk.users.writes

import io.branchtalk.shared.models.{ CreationScheduled, DeletionScheduled, UpdateScheduled }
import io.branchtalk.users.model.User

trait UserWrites[F[_]] {

  def createUser(newUser:     User.Create): F[CreationScheduled[User]]
  def updateUser(updatedUser: User.Update): F[UpdateScheduled[User]]
  def deleteUser(deletedUser: User.Delete): F[DeletionScheduled[User]]
}
