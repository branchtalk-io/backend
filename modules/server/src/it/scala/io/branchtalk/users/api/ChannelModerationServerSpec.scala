package io.branchtalk.users.api

import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, _ }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.mappings._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.{ Permission, RequiredPermissions, User }
import org.specs2.mutable.Specification

final class ChannelModerationServerSpec
    extends Specification
    with ServerIOTest
    with UsersFixtures
    with DiscussionsFixtures {

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "ChannelModerationServer-provided endpoints" should {

    // GET (pagination) moved to a separate test suite

    "on POST /discussions/channels/{channelID}/moderation" in {
      for {
        // given
        (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
          usersWrites.userWrites.createUser
        )
        _ <- usersReads.userReads.requireById(userID).eventually()
        _ <- usersWrites.userWrites.updateUser(
          User.Update(
            id = userID,
            moderatorID = None,
            newUsername = Updatable.Keep,
            newDescription = OptionUpdatable.Keep,
            newPassword = Updatable.Keep,
            updatePermissions = List(Permission.Update.Add(Permission.Administrate))
          )
        )
        (CreationScheduled(updatedUserID), CreationScheduled(_)) <- userCreate.flatMap(
          usersWrites.userWrites.createUser
        )
        _ <- usersReads.userReads.requireById(updatedUserID).eventually()
        channelID <- channelIDCreate
        permission = Permission.ModerateChannel(channelID)
        // when
        response <- ChannelModerationAPIs.grantChannelModeration.toTestCall.untupled(
          Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
          channelID,
          GrantModerationRequest(updatedUserID)
        )
        _ <- usersReads.userReads
          .requireById(updatedUserID)
          .assert("User should eventually have Moderator status")(
            _.data.permissions.allow(RequiredPermissions.one(permission))
          )
          .eventually()
      } yield
      // then
      response.body must beValid(beRight(be_===(GrantModerationResponse(updatedUserID))))
    }

    "on DELETE /discussions/channels/{channelID}/moderation" in {
      for {
        // given
        (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
          usersWrites.userWrites.createUser
        )
        _ <- usersReads.userReads.requireById(userID).eventually()
        _ <- usersWrites.userWrites.updateUser(
          User.Update(
            id = userID,
            moderatorID = None,
            newUsername = Updatable.Keep,
            newDescription = OptionUpdatable.Keep,
            newPassword = Updatable.Keep,
            updatePermissions = List(Permission.Update.Add(Permission.Administrate))
          )
        )
        (CreationScheduled(updatedUserID), CreationScheduled(_)) <- userCreate.flatMap(
          usersWrites.userWrites.createUser
        )
        _ <- usersReads.userReads.requireById(updatedUserID).eventually()
        channelID <- channelIDCreate
        permission = Permission.ModerateChannel(channelID)
        _ <- usersWrites.userWrites.updateUser(
          User.Update(
            id = updatedUserID,
            moderatorID = None,
            newUsername = Updatable.Keep,
            newDescription = OptionUpdatable.Keep,
            newPassword = Updatable.Keep,
            updatePermissions = List(Permission.Update.Add(permission))
          )
        )
        _ <- usersReads.userReads
          .requireById(updatedUserID)
          .assert("User should eventually have Moderator status")(
            _.data.permissions.allow(RequiredPermissions.one(permission))
          )
          .eventually()
        // when
        response <- ChannelModerationAPIs.revokeChannelModeration.toTestCall.untupled(
          Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
          channelID,
          RevokeModerationRequest(updatedUserID)
        )
        _ <- usersReads.userReads
          .requireById(updatedUserID)
          .assert("User should eventually lose Moderator status")(
            _.data.permissions.allow(RequiredPermissions.one(permission))
          )
          .eventually()
      } yield
      // then
      response.body must beValid(beRight(be_===(RevokeModerationResponse(updatedUserID))))
    }
  }
}
