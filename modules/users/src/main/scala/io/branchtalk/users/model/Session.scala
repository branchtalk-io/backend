package io.branchtalk.users.model

import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) final case class Session(
  id:   ID[Session],
  data: Session.Data
)
object Session extends SessionProperties {

  @Semi(FastEq, ShowPretty) final case class Data(
    userID: ID[User]
    // TODO: when it expires
    // TODO: does it have extra/only selected permissions, what kind of session is it
  )
}
