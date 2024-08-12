package io.branchtalk.openapi

import io.branchtalk.api.ServerIOTest
import io.branchtalk.shared.model.TestUUIDGenerator
import org.specs2.mutable.Specification
import sttp.model.StatusCode
import sttp.client3.*

final class OpenAPIServerSpec extends Specification, ServerIOTest {

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "OpenAPIServer" should {

    "return valid OpenAPI v3 specification" in {
      for {
        result <- basicRequest.get(sttpBaseUri.withWholePath("docs/swagger.json")).send(client)
      } yield result.code === StatusCode.Ok
    }
  }
}
