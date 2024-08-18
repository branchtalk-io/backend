package io.branchtalk.users.api

import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, _ }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.mappings.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels.*
import io.branchtalk.users.model.{ Ban, Permission, User }
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class UserBanServerListingSpec extends Specification, ServerIOTest, UsersFixtures, DiscussionsFixtures {

  // User pagination tests cannot be run in parallel to other User tests (no parent to filter other tests)
  sequential

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "UserBanServer-provided pagination endpoints" should {

    "on GET /users/bans" in {

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
            userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.unwrap)
          )
          _ <- bannedUserIDs.traverse(usersReads.userReads.requireById(_)).eventually()
          reason = Ban.Reason("test")
          _ <- bannedUserIDs
            .map(Ban.Order(_, reason, Ban.Scope.Globally, userID.some))
            .traverse(usersWrites.banWrites.orderBan)
          _ <- usersReads.banReads.findGlobally
            .assert("All Users should be Banned")(_.map(_.bannedUserID).toSet eqv bannedUserIDs.toSet)
            .eventually()
          // when
          response <- UserBanAPIs.list.toTestCall(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID))
          )
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[BansResponse]))
          response.body.toValidOpt.flatMap(_.toOption).map(_.bannedIDs.toSet === bannedUserIDs.toSet).getOrElse(pass)
        }
      }
    }
  }
}
