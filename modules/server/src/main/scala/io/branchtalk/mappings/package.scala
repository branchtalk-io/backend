package io.branchtalk

import cats.data.NonEmptySet
import io.branchtalk.api.UserID
import io.branchtalk.shared.model.ID
import monocle.Iso

import scala.collection.immutable.SortedSet
import scala.util.Try

package object mappings {

  // API <-> Users

  val usernameApi2Users: Iso[api.Username, users.model.User.Name] = Iso[api.Username, users.model.User.Name] {
    username => users.model.User.Name(username.nonEmptyString)
  }(username => api.Username(username.nonEmptyString))

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

  // scalastyle:off cyclomatic.complexity
  @SuppressWarnings(Array("org.wartremover.warts.Throw")) // too PITA to do it right
  def permissionApi2Users(owner: UserID): Iso[api.Permission, users.model.Permission] =
    Iso[api.Permission, users.model.Permission] {
      case api.Permission.Administrate =>
        users.model.Permission.Administrate
      case api.Permission.IsOwner =>
        users.model.Permission.IsUser(userIDApi2Users.get(owner))
      case api.Permission.ModerateUsers =>
        users.model.Permission.ModerateUsers
      case api.Permission.ModerateChannel(channelID) =>
        users.model.Permission.ModerateChannel(channelIDApi2Users.get(channelID))
      case api.Permission.CanPublish(channelID) =>
        users.model.Permission.CanPublish(channelIDApi2Users.get(channelID))
    } {
      case users.model.Permission.Administrate =>
        api.Permission.Administrate
      case users.model.Permission.IsUser(userID) if userID.uuid === owner.uuid && owner =!= UserID.empty =>
        api.Permission.IsOwner
      case users.model.Permission.IsUser(_) =>
        throw new Exception("Cannot map User to Owner if ID doesn't match current Owner ID")
      case users.model.Permission.ModerateUsers =>
        api.Permission.ModerateUsers
      case users.model.Permission.ModerateChannel(channelID) =>
        api.Permission.ModerateChannel(channelIDApi2Users.reverseGet(channelID))
      case users.model.Permission.CanPublish(channelID) =>
        api.Permission.CanPublish(channelIDApi2Users.reverseGet(channelID))
    }
  // scalastyle:on cyclomatic.complexity

  // scalastyle:off cyclomatic.complexity
  def requiredPermissionsApi2Users(owner: UserID): Iso[api.RequiredPermissions, users.model.RequiredPermissions] = {
    val permApi2Users = permissionApi2Users(owner)
    def safeReverseGet(perm: users.model.Permission) =
      Try(SortedSet(permApi2Users.reverseGet(perm))).getOrElse(SortedSet.empty[api.Permission])
    lazy val reqApi2Users: Iso[api.RequiredPermissions, users.model.RequiredPermissions] =
      Iso[api.RequiredPermissions, users.model.RequiredPermissions] {
        case api.RequiredPermissions.Empty =>
          users.model.RequiredPermissions.Empty
        case api.RequiredPermissions.AllOf(set) =>
          users.model.RequiredPermissions.AllOf(set.map(permApi2Users.get))
        case api.RequiredPermissions.AnyOf(set) =>
          users.model.RequiredPermissions.AnyOf(set.map(permApi2Users.get))
        case api.RequiredPermissions.And(x, y) =>
          users.model.RequiredPermissions.And(reqApi2Users.get(x), reqApi2Users.get(y))
        case api.RequiredPermissions.Or(x, y) =>
          users.model.RequiredPermissions.Or(reqApi2Users.get(x), reqApi2Users.get(y))
        case api.RequiredPermissions.Not(x) => users.model.RequiredPermissions.Not(reqApi2Users.get(x))
      } {
        case users.model.RequiredPermissions.Empty =>
          api.RequiredPermissions.Empty
        case users.model.RequiredPermissions.AllOf(set) =>
          NonEmptySet
            .fromSet(set.toSortedSet.flatMap(safeReverseGet))
            .fold[api.RequiredPermissions](api.RequiredPermissions.Empty)(api.RequiredPermissions.AllOf)
        case users.model.RequiredPermissions.AnyOf(set) =>
          NonEmptySet
            .fromSet(set.toSortedSet.flatMap(safeReverseGet))
            .fold[api.RequiredPermissions](api.RequiredPermissions.Empty)(api.RequiredPermissions.AnyOf)
        case users.model.RequiredPermissions.And(x, y) =>
          api.RequiredPermissions.And(reqApi2Users.reverseGet(x), reqApi2Users.reverseGet(y))
        case users.model.RequiredPermissions.Or(x, y) =>
          api.RequiredPermissions.Or(reqApi2Users.reverseGet(x), reqApi2Users.reverseGet(y))
        case users.model.RequiredPermissions.Not(x) => api.RequiredPermissions.Not(reqApi2Users.reverseGet(x))
      }
    reqApi2Users
  }
  // scalastyle:on cyclomatic.complexity

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
