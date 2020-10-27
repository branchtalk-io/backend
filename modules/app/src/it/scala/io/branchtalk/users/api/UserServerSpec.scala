package io.branchtalk.users.api

import cats.effect.IO
import eu.timepit.refined.auto._
import io.branchtalk.api._
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.mappings._
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.{ Password, Session, User }
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class UserServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  protected implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  "UserServer-provided endpoints" should {

    "on POST /users/sign_up" in {

      "schedule User and Session creation on valid JSON" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              password <- Password.Raw.parse[IO]("test".getBytes)
              creationData <- userCreate
              // when
              response <- UserAPIs.signUp.toTestCall(
                SignUpRequest(
                  email       = creationData.email,
                  username    = creationData.username,
                  description = creationData.description,
                  password    = password
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
            }
        }
      }
    }

    "on POST /users/sign_in" in {

      "log User in on valid credentials" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              password <- Password.Raw.parse[IO]("test".getBytes)
              creationData <- userCreate.map(_.copy(password = Password.create(password)))
              toCreate <- usersWrites.userWrites.createUser(creationData)
              _ <- usersReads.userReads.requireById(toCreate._1.id).eventually()
              _ <- usersReads.sessionReads.requireSession(toCreate._2.id).eventually()
              // when
              sessionResponse <- UserAPIs.signIn.toTestCall(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(toCreate._2.id))
              )
              credentialsResponse <- UserAPIs.signIn.toTestCall(
                Authentication.Credentials(
                  username = usernameApi2Users.reverseGet(creationData.username),
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
  }
}
