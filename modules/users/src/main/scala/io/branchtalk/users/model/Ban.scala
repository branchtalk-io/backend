package io.branchtalk.users.model

import io.branchtalk.shared.model.ID

final case class Ban(bannedUserID: ID[User], reason: Ban.Reason, scope: Ban.Scope)
object Ban extends BanProperties with BanCommands
