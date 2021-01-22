package io.branchtalk.users.reads

import io.branchtalk.shared.model.ID
import io.branchtalk.users.model.{ Ban, Channel, User }

trait BanReads[F[_]] {

  def findForUser(userID:       ID[User]):    F[Set[Ban]]
  def findForChannel(channelID: ID[Channel]): F[Set[Ban]]
  def findGlobally: F[Set[Ban]]
}
