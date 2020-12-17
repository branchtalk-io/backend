package io.branchtalk.users.model

import io.branchtalk.shared.model.{ ID, UUID }
import io.scalaland.chimney.dsl._

final case class BanDao(
  bannedUserID: ID[User],
  banType:      Ban.Scope.Type,
  banID:        Option[UUID],
  reason:       Ban.Reason
) {

  def toDomain: Ban =
    this.into[Ban].withFieldConst(_.scope, Ban.Scope.Tupled(banType, banID)).transform
}
object BanDao {

  def fromDomain(ban: Ban): BanDao = {
    val (banType, banID) = Ban.Scope.Tupled.unpack(ban.scope)
    ban.into[BanDao].withFieldConst(_.banType, banType).withFieldConst(_.banID, banID).transform
  }
}
