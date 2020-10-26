package io.branchtalk.users.api

import cats.effect.IO
import eu.timepit.refined.auto._
import io.branchtalk.api.ServerIOTest
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.Password
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
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              userData <- userCreate
              password <- Password.Raw.parse[IO]("test".getBytes)
              result <- UserAPIs.signUp.toTestCall(
                SignUpRequest(
                  email       = userData.email,
                  username    = userData.username,
                  description = userData.description,
                  password    = password
                )
              )
            } yield {
              result.code must_=== StatusCode.Ok
              result.body must beValid(beRight(anInstanceOf[SignUpResponse]))
            }
        }
      }
    }
  }
}
