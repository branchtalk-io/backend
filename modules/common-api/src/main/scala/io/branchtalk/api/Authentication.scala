package io.branchtalk.api

import io.branchtalk.ADT

sealed trait Authentication extends ADT {

  def fold[B](session: SessionID => B, credentials: (Username, Password) => B): B = this match {
    case Authentication.Session(sessionID)              => session(sessionID)
    case Authentication.Credentials(username, password) => credentials(username, password)
  }
}
object Authentication {

  final case class Session(sessionID:    SessionID) extends Authentication
  final case class Credentials(username: Username, password: Password) extends Authentication
}
