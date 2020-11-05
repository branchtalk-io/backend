package io.branchtalk.openapi

import io.branchtalk.api.ServerIOTest
import io.branchtalk.shared.models.TestUUIDGenerator
import org.specs2.mutable.Specification
import sttp.model.StatusCode
import sttp.client._

final class OpenAPIServerSpec extends Specification with ServerIOTest {

  protected implicit val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "OpenAPIServer" should {

    "return valid OpenAPI v3 specification" in {
      (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
        case (usersProjector, discussionsProjector) =>
          for {
            _ <- usersProjector.logError("Error reported by Users projector").start
            _ <- discussionsProjector.logError("Error reported by Discussions projector").start
            result <- basicRequest.get(sttpBaseUri.path("docs/swagger.json")).send()(client, implicitly)
          } yield {
            result.code must_=== StatusCode.Ok
          }
      }
    }
  }
}
