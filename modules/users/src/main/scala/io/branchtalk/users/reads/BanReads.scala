package io.branchtalk.users.reads

import io.branchtalk.shared.model.ID
import io.branchtalk.users.model.{ Ban, User }

trait BanReads[F[_]] {

  def findForUser(userID: ID[User]): F[Set[Ban]]
}
