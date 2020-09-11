package io.branchtalk.api

import io.branchtalk.ADT

sealed trait Authentication extends ADT
object Authentication {

  final case class Session(sessionID:    SessionID) extends Authentication
  final case class Credentials(username: Username, password: Password) extends Authentication
}
