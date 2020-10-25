package io.branchtalk.users.api

import io.branchtalk.api.ServerIOTest
import io.branchtalk.shared.models.UUIDGenerator
import org.specs2.mutable.Specification

final class UserServerSpec extends Specification with ServerIOTest {

  protected implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  "UserServer" should {

    "on POST /users/sign_up" in {
      true must beTrue // TODO
    }
  }
}
