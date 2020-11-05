package io.branchtalk.users.api

import cats.effect.IO
import io.branchtalk.api._
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CreationScheduled, OptionUpdatable, TestUUIDGenerator, Updatable }
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.{ Password, Session, User }
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class UserServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "UserServer-provided endpoints" should {

    "on POST /users" in {

      "schedule User and Session creation on valid JSON" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
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
              session <- possibleResult.map(_.sessionID).traverse(usersReads.sessionReads.requireSession).eventually()
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(anInstanceOf[SignUpResponse]))
              user must beSome(anInstanceOf[User])
              session must beSome(anInstanceOf[Session])
              (user, session, response.body.toOption.flatMap(_.toOption))
                .mapN((u, s, r) => r must_=== SignUpResponse(u.id, s.id))
                .getOrElse(true must beTrue)
            }
        }
      }
    }

    "on POST /users/session" in {

      "log User in on valid credentials" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              password <- Password.Raw.parse[IO]("password".getBytes)
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate
                .map(_.copy(password = Password.create(password)))
                .flatMap(usersWrites.userWrites.createUser)
              user <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
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
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              password <- Password.Raw.parse[IO]("password".getBytes)
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate
                .map(_.copy(password = Password.create(password)))
                .flatMap(usersWrites.userWrites.createUser)
              user <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
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
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              user <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              // when
              response <- UserAPIs.fetchProfile.toTestCall(userID)
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(be_===(APIUser.fromDomain(user))))
            }
        }
      }
    }

    "on POST /users/{userID}" in {

      "should update User's profile if Session belongs to them" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              user <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              newUsername <- User.Name.parse[IO]("new test name")
              newDescription = User.Description("new test description")
              newPassword <- Password.Raw.parse[IO]("new password".getBytes)
              // when
              response <- UserAPIs.updateProfile.toTestCall(
                (Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
                 userID,
                 UpdateUserRequest(
                   newUsername = Updatable.Set(newUsername),
                   newDescription = OptionUpdatable.Set(newDescription),
                   newPassword = Updatable.Set(newPassword)
                 )
                )
              )
              updatedUser <- usersReads.userReads
                .requireById(userID)
                .flatTap { current =>
                  IO(assert(current.data.lastModifiedAt.isDefined, "Updated entity should have lastModifiedAt set"))
                }
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
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              // when
              response <- UserAPIs.deleteProfile.toTestCall(
                (Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)), userID)
              )
              _ <- usersReads.userReads
                .deleted(userID)
                .assert("User should be eventually deleted")(identity)
                .eventually()
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
