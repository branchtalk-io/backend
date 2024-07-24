package io.branchtalk.api

enum Authentication {
  case Session(sessionID: SessionID)
  case Credentials(username: Username, password: Password)

  def fold[B](session: SessionID => B, credentials: (Username, Password) => B): B = this match {
    case Authentication.Session(sessionID)              => session(sessionID)
    case Authentication.Credentials(username, password) => credentials(username, password)
  }
}
