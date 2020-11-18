package io.branchtalk.users.model

import io.branchtalk.shared.model.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) final case class Session(
  id:   ID[Session],
  data: Session.Data
)
object Session extends SessionProperties with SessionCommands {

  @Semi(FastEq, ShowPretty) final case class Data(
    userID:    ID[User],
    usage:     Session.Usage,
    expiresAt: Session.ExpirationTime
  )
}
