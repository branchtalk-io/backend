package io.branchtalk.users.api

import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, _ }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.mappings._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.{ Ban, Permission, User }
import org.specs2.mutable.Specification

final class UserBanServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "UserBanServer-provided endpoints" should {

    // GET (pagination) moved to a separate test suite

    "on POST /users/bans" in {
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
        (CreationScheduled(bannedUserID), CreationScheduled(_)) <- userCreate.flatMap(
          usersWrites.userWrites.createUser
        )
        _ <- usersReads.userReads.requireById(bannedUserID).eventually()
        reason = Ban.Reason("test")
        // when
        response <- UserBanAPIs.orderBan.toTestCall.untupled(
          Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
          BanOrderRequest(bannedUserID, reason)
        )
        _ = println(response)
        bans <- usersReads.banReads
          .findForUser(bannedUserID)
          .assert("User should be eventually banned")(_.nonEmpty)
          .eventually()
      } yield {
        // then
        response.body must beValid(beRight(be_===(BanOrderResponse(bannedUserID))))
        bans must_=== Set(Ban(bannedUserID, reason, Ban.Scope.Globally))
      }
    }

    "on DELETE /users/bans" in {
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
        (CreationScheduled(bannedUserID), CreationScheduled(_)) <- userCreate.flatMap(
          usersWrites.userWrites.createUser
        )
        _ <- usersReads.userReads.requireById(bannedUserID).eventually()
        reason = Ban.Reason("test")
        _ <- usersWrites.banWrites.orderBan(
          Ban.Order(bannedUserID, reason, Ban.Scope.Globally, userID.some)
        )
        _ <- usersReads.banReads
          .findForUser(bannedUserID)
          .assert("User should be eventually banned")(_.nonEmpty)
          .eventually()
        // when
        response <- UserBanAPIs.liftBan.toTestCall.untupled(
          Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
          BanLiftRequest(bannedUserID)
        )
        _ <- usersReads.banReads
          .findForUser(bannedUserID)
          .assert("User should be eventually unbanned")(_.isEmpty)
          .eventually()
      } yield
      // then
      response.body must beValid(beRight(be_===(BanLiftResponse(bannedUserID))))
    }
  }
}
