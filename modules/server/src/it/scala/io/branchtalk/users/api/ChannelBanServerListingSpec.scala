package io.branchtalk.users.api

import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, _ }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.mappings._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.{ Ban, Permission, User }
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class ChannelBanServerListingSpec
    extends Specification
    with ServerIOTest
    with UsersFixtures
    with DiscussionsFixtures {

  // User pagination tests cannot be run in parallel to other User tests (no parent to filter other tests)
  sequential

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "ChannelBanServer-provided pagination endpoints" should {

    "on GET /discussions/channels/{channelID}/bans" in {

      "return listed banned Users" in {
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
          bannedUserIDs <- (0 until 9).toList.traverse(_ =>
            userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
          )
          _ <- bannedUserIDs.traverse(usersReads.userReads.requireById(_)).eventually()
          channelID <- channelIDCreate
          reason = Ban.Reason("test")
          _ <- bannedUserIDs
            .map(Ban.Order(_, reason, Ban.Scope.ForChannel(channelID), userID.some))
            .traverse(usersWrites.banWrites.orderBan)
          _ <- usersReads.banReads
            .findForChannel(channelID)
            .assert("All Users should be Banned")(_.map(_.bannedUserID).toSet === bannedUserIDs.toSet)
            .eventually()
          // when
          response <- ChannelBanAPIs.list.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID
          )
        } yield {
          // then
          response.code must_=== StatusCode.Ok
          response.body must beValid(beRight(anInstanceOf[BansResponse]))
          response.body.toValidOpt
            .flatMap(_.toOption)
            .map(_.bannedIDs.toSet must_=== bannedUserIDs.toSet)
            .getOrElse(pass)
        }
      }
    }
  }
}
