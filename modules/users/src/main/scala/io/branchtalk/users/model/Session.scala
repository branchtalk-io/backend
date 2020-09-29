package io.branchtalk.users.model

import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) final case class Session(
  id:   ID[Session],
  data: Session.Data
)
object Session extends SessionProperties with SessionCommands {

  @Semi(FastEq, ShowPretty) final case class Data(
    userID:    ID[User],
    ofType:    Session.Type,
    expiresAt: Session.ExpirationTime
  )
}
