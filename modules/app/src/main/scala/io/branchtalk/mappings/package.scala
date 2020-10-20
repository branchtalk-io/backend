package io.branchtalk

import io.branchtalk.shared.models.ID
import monocle.Iso

package object mappings {

  // API <-> Users

  val usernameApi2Users: Iso[api.Username, users.model.User.Name] = Iso[api.Username, users.model.User.Name] {
    username => users.model.User.Name(username.nonEmptyString)
  }(username => api.Username(username.string))

  val passwordApi2Users: Iso[api.Password, users.model.Password.Raw] = Iso[api.Password, users.model.Password.Raw] {
    password => users.model.Password.Raw(password.nonEmptyBytes)
  }(password => api.Password(password.nonEmptyBytes))

  val sessionIDApi2Users: Iso[api.SessionID, ID[users.model.Session]] = Iso[api.SessionID, ID[users.model.Session]] {
    sessionID => ID[users.model.Session](sessionID.uuid)
  }(sessionID => api.SessionID(sessionID.uuid))

  val userIDApi2Users: Iso[api.UserID, ID[users.model.User]] = Iso[api.UserID, ID[users.model.User]] { userID =>
    ID[users.model.User](userID.uuid)
  }(userID => api.UserID(userID.uuid))

  val channelIDApi2Users: Iso[api.ChannelID, ID[users.model.Channel]] = Iso[api.ChannelID, ID[users.model.Channel]] {
    channelID => ID[users.model.Channel](channelID.uuid)
  }(channelID => api.ChannelID(channelID.uuid))

  val permissionApi2Users: Iso[api.Permission, users.model.Permission] = Iso[api.Permission, users.model.Permission] {
    case api.Permission.EditProfile(userID) =>
      users.model.Permission.EditProfile(userIDApi2Users.get(userID))
    case api.Permission.ModerateChannel(channelID) =>
      users.model.Permission.ModerateChannel(channelIDApi2Users.get(channelID))
    case api.Permission.ModerateUsers =>
      users.model.Permission.ModerateUsers
  } {
    case users.model.Permission.EditProfile(userID) =>
      api.Permission.EditProfile(userIDApi2Users.reverseGet(userID))
    case users.model.Permission.ModerateChannel(channelID) =>
      api.Permission.ModerateChannel(channelIDApi2Users.reverseGet(channelID))
    case users.model.Permission.ModerateUsers =>
      api.Permission.ModerateUsers
  }

  // API <-> Discussions

  val userIDApi2Discussions: Iso[api.UserID, ID[discussions.model.User]] = Iso[api.UserID, ID[discussions.model.User]] {
    userID => ID[discussions.model.User](userID.uuid)
  }(id => api.UserID(id.uuid))

  val channelIDApi2Discussions: Iso[api.ChannelID, ID[discussions.model.Channel]] =
    Iso[api.ChannelID, ID[discussions.model.Channel]](userID => ID[discussions.model.Channel](userID.uuid))(id =>
      api.ChannelID(id.uuid)
    )

  // Users <-> Discussions

  val userIDUsers2Discussions: Iso[ID[users.model.User], ID[discussions.model.User]] =
    Iso[ID[users.model.User], ID[discussions.model.User]](userID => ID[discussions.model.User](userID.uuid)) { userID =>
      ID[users.model.User](userID.uuid)
    }

  val channelIDUsers2Discussions: Iso[ID[users.model.Channel], ID[discussions.model.Channel]] =
    Iso[ID[users.model.Channel], ID[discussions.model.Channel]] { channelID =>
      ID[discussions.model.Channel](channelID.uuid)
    }(channelID => ID[users.model.Channel](channelID.uuid))
}
