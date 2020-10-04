package io.branchtalk.users

import cats.effect.{IO, Resource}
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.{IOTest, ResourcefulTest}
import org.specs2.mutable.Specification

final class UserReadsWritesSpec  extends Specification with IOTest with ResourcefulTest with UsersFixtures {

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  // populated by resources
  private var usersReads:  UsersReads[IO]  = _
  private var usersWrites: UsersWrites[IO] = _

  override protected def testResource: Resource[IO, Unit] =
    for {
      domainCfg <- TestUsersConfig.loadDomainConfig[IO]
      reads <- UsersModule.reads[IO](domainCfg)
      writes <- UsersModule.writes[IO](domainCfg)
    } yield {
      usersReads  = reads
      usersWrites = writes
    }

  "User Reads & Writes" should {

    "create a User and eventually read it" in {
      // TODO
      true must beTrue
      true must beTrue
    }

    "don't update a User that doesn't exists" in {
      // TODO
      true must beTrue
      true must beTrue
    }

    "update an existing User" in {
      // TODO
      true must beTrue
      true must beTrue
    }

    "allow delete of a created User" in {
      // TODO
      true must beTrue
      true must beTrue
    }
  }
}
