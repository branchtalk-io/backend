package io.branchtalk.users.reads

import io.branchtalk.shared.models.ID
import io.branchtalk.users.model.Session

trait SessionReads[F[_]] {

  def requireSession(id: ID[Session]): F[Session]
}
