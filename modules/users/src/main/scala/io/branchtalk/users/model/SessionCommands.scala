package io.branchtalk.users.model

import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

trait SessionCommands {
  type Create = SessionCommands.Create
  type Delete = SessionCommands.Delete
  val Create = SessionCommands.Create
  val Delete = SessionCommands.Delete
}
object SessionCommands {

  @Semi(FastEq, ShowPretty) final case class Create(
    userID:    ID[User],
    ofType:    Session.Type,
    expiresAt: Session.ExpirationTime
  )

  @Semi(FastEq, ShowPretty) final case class Delete(
    id: ID[Session]
  )
}
