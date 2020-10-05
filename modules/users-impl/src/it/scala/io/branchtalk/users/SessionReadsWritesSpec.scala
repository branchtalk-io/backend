package io.branchtalk.users

import cats.effect.{ IO, Resource }
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.{ IOTest, ResourcefulTest }
import org.specs2.mutable.Specification

final class SessionReadsWritesSpec extends Specification with IOTest with ResourcefulTest with UsersFixtures {

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

  "Session Reads & Writes" should {

    "create a Session and immediately read it" in {
      // TODO
      true must beTrue
      true must beTrue
    }

    "allow delete of a created Session" in {
      // TODO
      true must beTrue
      true must beTrue
    }
  }
}
