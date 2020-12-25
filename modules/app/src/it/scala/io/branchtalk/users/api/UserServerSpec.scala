package io.branchtalk.users.api

import cats.effect.IO
import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, _ }
import io.branchtalk.api.TapirSupport._
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.mappings._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.{ Password, Permission, RequiredPermissions, Session, User }
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class UserServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  sequential // User pagination tests cannot be run in parallel to other User tests

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "UserServer-provided endpoints" should {

    "on GET /users" in {

      "return paginated Users" in {
        withAllProjections {
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
              .traverse(_ => userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id))
              .map(_ :+ userID)
            users <- userIDs.traverse(usersReads.userReads.requireById(_)).eventually()
            // when
            response1 <- UserAPIs.paginate.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              None,
              PaginationLimit(5).some
            )
            response2 <- UserAPIs.paginate.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              PaginationOffset(5L).some,
              PaginationLimit(5).some
            )
          } yield {
            // then
            response1.code must_=== StatusCode.Ok
            response1.body must beValid(beRight(anInstanceOf[Pagination[APIUser]]))
            response2.code must_=== StatusCode.Ok
            response2.body must beValid(beRight(anInstanceOf[Pagination[APIUser]]))
            (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
              .mapN { (pagination1, pagination2) =>
                (pagination1.entities.toSet ++ pagination2.entities.toSet) must_=== users.map(APIUser.fromDomain).toSet
              }
              .getOrElse(pass)
          }
        }
      }
    }

    "on GET /users/newest" in {

      "return newest Users" in {
        withAllProjections {
          for {
            // given
            _ <- usersReads.userReads.paginate(User.Sorting.NameAlphabetically, 0L, 1000).flatMap {
              case Paginated(entities, _) =>
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
              .traverse(_ => userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id))
              .map(_ :+ userID)
            users <- userIDs.traverse(usersReads.userReads.requireById(_)).eventually()
            // when
            response1 <- UserAPIs.newest.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              None,
              PaginationLimit(5).some
            )
            response2 <- UserAPIs.newest.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              PaginationOffset(5L).some,
              PaginationLimit(5).some
            )
          } yield {
            // then
            response1.code must_=== StatusCode.Ok
            response1.body must beValid(beRight(anInstanceOf[Pagination[APIUser]]))
            response2.code must_=== StatusCode.Ok
            response2.body must beValid(beRight(anInstanceOf[Pagination[APIUser]]))
            (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
              .mapN { (pagination1, pagination2) =>
                (pagination1.entities.toSet ++ pagination2.entities.toSet) must_=== users.map(APIUser.fromDomain).toSet
              }
              .getOrElse(pass)
          }
        }
      }
    }

    "on GET /users/sessions" in {

      "return newest Sessions" in {
        withAllProjections {
          for {
            // given
            _ <- usersReads.userReads.paginate(User.Sorting.NameAlphabetically, 0L, 1000).flatMap {
              case Paginated(entities, _) =>
                entities.traverse_(user => usersWrites.userWrites.deleteUser(User.Delete(user.id, None)))
            }
            (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
              usersWrites.userWrites.createUser
            )
            _ <- usersReads.userReads.requireById(userID).eventually()
            sessionIDs <- (0 until 9).toList
              .traverse(_ => sessionCreate(userID).flatMap(usersWrites.sessionWrites.createSession).map(_.id))
              .map(sessionID +: _)
            sessions <- sessionIDs.traverse(usersReads.sessionReads.requireById(_)).eventually()
            // when
            response1 <- UserAPIs.sessions.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              None,
              PaginationLimit(5).some
            )
            response2 <- UserAPIs.sessions.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              PaginationOffset(5L).some,
              PaginationLimit(5).some
            )
          } yield {
            // then
            response1.code must_=== StatusCode.Ok
            response1.body must beValid(beRight(anInstanceOf[Pagination[APISession]]))
            response2.code must_=== StatusCode.Ok
            response2.body must beValid(beRight(anInstanceOf[Pagination[APISession]]))
            (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
              .mapN { (pagination1, pagination2) =>
                (pagination1.entities.toSet ++ pagination2.entities.toSet) must_=== sessions
                  .map(APISession.fromDomain)
                  .toSet
              }
              .getOrElse(pass)
          }
        }
      }
    }

    "on POST /users" in {

      "schedule User and Session creation on valid JSON" in {
        withAllProjections {
          for {
            // given
            password <- Password.Raw.parse[IO]("password".getBytes)
            creationData <- userCreate
            // when
            response <- UserAPIs.signUp.toTestCall(
              SignUpRequest(
                email = creationData.email,
                username = creationData.username,
                description = creationData.description,
                password = password
              )
            )
            possibleResult = response.body.toOption.flatMap(_.toOption)
            user <- possibleResult.map(_.userID).traverse(usersReads.userReads.requireById).eventually()
            session <- possibleResult.map(_.sessionID).traverse(usersReads.sessionReads.requireById).eventually()
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(anInstanceOf[SignUpResponse]))
            user must beSome(anInstanceOf[User])
            session must beSome(anInstanceOf[Session])
            (user, session, response.body.toOption.flatMap(_.toOption))
              .mapN((u, s, r) => r must_=== SignUpResponse(u.id, s.id))
              .getOrElse(pass)
          }
        }
      }
    }

    "on POST /users/session" in {

      "log User in on valid credentials" in {
        withAllProjections {
          for {
            // given
            password <- Password.Raw.parse[IO]("password".getBytes)
            (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate
              .map(_.copy(password = Password.create(password)))
              .flatMap(usersWrites.userWrites.createUser)
            user <- usersReads.userReads.requireById(userID).eventually()
            _ <- usersReads.sessionReads.requireById(sessionID).eventually()
            // when
            sessionResponse <- UserAPIs.signIn.toTestCall(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID))
            )
            credentialsResponse <- UserAPIs.signIn.toTestCall(
              Authentication.Credentials(
                username = usernameApi2Users.reverseGet(user.data.username),
                password = passwordApi2Users.reverseGet(password)
              )
            )
          } yield {
            // then
            sessionResponse.code must_=== StatusCode.Ok
            sessionResponse.body must beValid(beRight(anInstanceOf[SignInResponse]))
            credentialsResponse.code must_=== StatusCode.Ok
            credentialsResponse.body must beValid(beRight(anInstanceOf[SignInResponse]))
          }
        }
      }
    }

    "on DELETE /users/session" in {

      "log out User" in {
        withAllProjections {
          for {
            // given
            password <- Password.Raw.parse[IO]("password".getBytes)
            (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate
              .map(_.copy(password = Password.create(password)))
              .flatMap(usersWrites.userWrites.createUser)
            user <- usersReads.userReads.requireById(userID).eventually()
            _ <- usersReads.sessionReads.requireById(sessionID).eventually()
            // when
            sessionResponse <- UserAPIs.signOut.toTestCall(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID))
            )
            credentialsResponse <- UserAPIs.signOut.toTestCall(
              Authentication.Credentials(
                username = usernameApi2Users.reverseGet(user.data.username),
                password = passwordApi2Users.reverseGet(password)
              )
            )
          } yield {
            // then
            sessionResponse.code must_=== StatusCode.Ok
            sessionResponse.body must beValid(beRight(be_===(SignOutResponse(userID, sessionID.some))))
            credentialsResponse.code must_=== StatusCode.Ok
            credentialsResponse.body must beValid(beRight(be_===(SignOutResponse(userID, None))))
          }
        }
      }
    }

    "on GET /users/{userID}" in {

      "should return User's profile" in {
        withAllProjections {
          for {
            // given
            (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
              usersWrites.userWrites.createUser
            )
            user <- usersReads.userReads.requireById(userID).eventually()
            _ <- usersReads.sessionReads.requireById(sessionID).eventually()
            // when
            response <- UserAPIs.fetchProfile.toTestCall.untupled(None, userID)
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(be_===(APIUser.fromDomain(user))))
          }
        }
      }
    }

    "on PUT /users/{userID}" in {

      "should update User's profile if Session belongs to them" in {
        withAllProjections {
          for {
            // given
            (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
              usersWrites.userWrites.createUser
            )
            user <- usersReads.userReads.requireById(userID).eventually()
            _ <- usersReads.sessionReads.requireById(sessionID).eventually()
            newUsername <- User.Name.parse[IO]("new test name")
            newDescription = User.Description("new test description")
            newPassword <- Password.Raw.parse[IO]("new password".getBytes)
            // when
            response <- UserAPIs.updateProfile.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              userID,
              UpdateUserRequest(
                newUsername = Updatable.Set(newUsername),
                newDescription = OptionUpdatable.Set(newDescription),
                newPassword = Updatable.Set(newPassword)
              )
            )
            updatedUser <- usersReads.userReads
              .requireById(userID)
              .assert("Updated entity should have lastModifiedAt set")(_.data.lastModifiedAt.isDefined)
              .eventually()
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(be_===(UpdateUserResponse(userID))))
            updatedUser must_=== user
              .lens(_.data.username)
              .set(newUsername)
              .lens(_.data.description)
              .set(newDescription.some)
              .lens(_.data.password)
              .set(updatedUser.data.password) // updated Password might have a different salt...
              .lens(_.data.lastModifiedAt)
              .set(updatedUser.data.lastModifiedAt)
            updatedUser.data.password.verify(newPassword) must beTrue // ...which is why we test it separately
          }
        }
      }
    }

    "on DELETE /users/{userID}" in {

      "should update User's profile if Session belongs to them" in {
        withAllProjections {
          for {
            // given
            (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
              usersWrites.userWrites.createUser
            )
            _ <- usersReads.userReads.requireById(userID).eventually()
            _ <- usersReads.sessionReads.requireById(sessionID).eventually()
            // when
            response <- UserAPIs.deleteProfile.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              userID
            )
            _ <- usersReads.userReads.deleted(userID).assert("User should be eventually deleted")(identity).eventually()
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(be_===(DeleteUserResponse(userID))))
          }
        }
      }
    }
  }
}
