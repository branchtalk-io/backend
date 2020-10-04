package io.branchtalk.users.reads

import io.branchtalk.shared.models.ID
import io.branchtalk.users.model.Session

trait SessionReads[F[_]] {

  // TODO: fetch (paginate?) all user's sessions
  def requireSession(id: ID[Session]): F[Session]
}
