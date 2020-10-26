package io.branchtalk.openapi

import cats.effect.IO
import io.branchtalk.api.ServerIOTest
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.users.api.UserAPIs
import io.branchtalk.users.api.UserModels.SignUpRequest
import io.branchtalk.users.model.Password
import org.http4s.dsl.request
import org.specs2.mutable.Specification
import sttp.model.StatusCode
import sttp.client._

final class OpenAPIServerSpec extends Specification with ServerIOTest {

  protected implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  "OpenAPIServer" should {

    "return valid OpenAPI v3 specification" in {
      (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
        case (usersProjector, discussionsProjector) =>
          for {
            _ <- usersProjector.logError("Error reported by Users projector").start
            _ <- discussionsProjector.logError("Error reported by Discussions projector").start
//            _ = basic.
          } yield {
//            println(result)
//            println(result.body)
//            result.code must_=== StatusCode.Ok
//            //              result.body.
            true must beTrue // TODO
          }
      }
    }
  }
}
