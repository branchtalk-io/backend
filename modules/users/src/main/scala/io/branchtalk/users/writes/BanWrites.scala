package io.branchtalk.users.writes

import io.branchtalk.users.model.Ban

trait BanWrites[F[_]] {

  def orderBan(order: Ban.Order): F[Unit]
  def liftBan(lift:   Ban.Lift):  F[Unit]
}
