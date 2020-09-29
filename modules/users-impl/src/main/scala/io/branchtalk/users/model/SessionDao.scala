package io.branchtalk.users.model

import io.branchtalk.shared.models.ID
import io.scalaland.chimney.dsl._

final case class SessionDao(
  id:               ID[Session],
  userID:           ID[User],
  usageType:        Session.Usage.Type,
  usagePermissions: Permissions,
  expiresAt:        Session.ExpirationTime
) {

  def toDomain: Session = Session(
    id   = id,
    data = this.into[Session.Data].withFieldConst(_.usage, Session.Usage.Tupled(usageType, usagePermissions)).transform
  )
}
object SessionDao {

  def fromDomain(session: Session): SessionDao = {
    val (usageType, usagePermissions) = Session.Usage.Tupled.unpack(session.data.usage)
    session.data
      .into[SessionDao]
      .withFieldConst(_.id, session.id)
      .withFieldConst(_.usageType, usageType)
      .withFieldConst(_.usagePermissions, usagePermissions)
      .transform
  }
}
