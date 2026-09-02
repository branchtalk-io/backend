package io.branchtalk.users.api

import cats.effect.IO
import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, * }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.mappings.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels.*
import io.branchtalk.users.model.{ Permission, RequiredPermissions, Session, User }
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class UserServerPaginationSpec extends Specification, ServerIOTest, UsersFixtures, DiscussionsFixtures {

  // User pagination tests cannot be run in parallel to other User tests (no parent to filter other tests)
  sequential

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "UserServer-provided pagination endpoints" should {

    "on GET /users" in {

      "return paginated Users" in {
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
              updatePermissions = List(Permission.Update.Add(Permission.ModerateUsers))
            )
          )
          _ <- usersReads.userReads
            .requireById(userID)
            .assert("User should eventually have Moderator status")(
              _.data.permissions.allow(RequiredPermissions.one(Permission.ModerateUsers))
            )
            .eventually()
          userIDs <- (0 until 9).toList
            .traverse(_ => userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.unwrap))
            .map(_ :+ userID)
          users <- userIDs.traverse(usersReads.userReads.requireById(_)).eventually()
          // when
          response1 <- UserAPIs.paginate.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            None,
            Pagination.Limit(5).some
          )
          response2 <- UserAPIs.paginate.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            Pagination.Offset(5L).some,
            Pagination.Limit(5).some
          )
        } yield {
          // then
          response1.code === StatusCode.Ok
          response1.body must beValid(beRight(beAnInstanceOf[Pagination[APIUser]]))
          response2.code === StatusCode.Ok
          response2.body must beValid(beRight(beAnInstanceOf[Pagination[APIUser]]))
          (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
            .mapN { (pagination1, pagination2) =>
              (pagination1.entities.toSet ++ pagination2.entities.toSet) === users.map(APIUser.fromDomain).toSet
            }
            .getOrElse(pass)
        }
      }

      "fail with NoPermission when called by a non-moderator" in {
        for {
          // given - create a regular user without ModerateUsers permission
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          // when - call paginate without moderator permissions
          response <- UserAPIs.paginate.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            None,
            None
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[UserError.NoPermission]))
        }
      }

      "fail with BadCredentials when called with an invalid session" in {
        for {
          // given
          fakeSessionID <- ID.create[IO, Session]
          // when
          response <- UserAPIs.paginate.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(fakeSessionID)),
            None,
            None
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[UserError.BadCredentials]))
        }
      }
    }

    "on GET /users/newest" in {

      "return newest Users" in {
        for {
          // given
          _ <- usersReads.userReads
            .paginate(User.Sorting.NameAlphabetically, Paginated.Offset(0L), Paginated.Limit(1000))
            .flatMap { case Paginated(entities, _) =>
              entities.traverse_(user => usersWrites.userWrites.deleteUser(User.Delete(user.id, None)))
            }
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
              updatePermissions = List(Permission.Update.Add(Permission.ModerateUsers))
            )
          )
          _ <- usersReads.userReads
            .requireById(userID)
            .assert("User should eventually have Moderator status")(
              _.data.permissions.allow(RequiredPermissions.one(Permission.ModerateUsers))
            )
            .eventually()
          userIDs <- (0 until 9).toList
            .traverse(_ => userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.unwrap))
            .map(_ :+ userID)
          users <- userIDs.traverse(usersReads.userReads.requireById(_)).eventually()
          // when
          response1 <- UserAPIs.newest.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            None,
            Pagination.Limit(5).some
          )
          response2 <- UserAPIs.newest.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            Pagination.Offset(5L).some,
            Pagination.Limit(5).some
          )
        } yield {
          // then
          response1.code === StatusCode.Ok
          response1.body must beValid(beRight(beAnInstanceOf[Pagination[APIUser]]))
          response2.code === StatusCode.Ok
          response2.body must beValid(beRight(beAnInstanceOf[Pagination[APIUser]]))
          (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
            .mapN { (pagination1, pagination2) =>
              (pagination1.entities.toSet ++ pagination2.entities.toSet) === users.map(APIUser.fromDomain).toSet
            }
            .getOrElse(pass)
        }
      }

      "fail with NoPermission when called by a non-moderator" in {
        for {
          // given - create a regular user without ModerateUsers permission
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          // when - call newest without moderator permissions
          response <- UserAPIs.newest.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            None,
            None
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[UserError.NoPermission]))
        }
      }

      "fail with BadCredentials when called with an invalid session" in {
        for {
          // given
          fakeSessionID <- ID.create[IO, Session]
          // when
          response <- UserAPIs.newest.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(fakeSessionID)),
            None,
            None
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[UserError.BadCredentials]))
        }
      }
    }
  }
}
