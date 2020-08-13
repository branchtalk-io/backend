package io.branchtalk.discussions.writes

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsWrites, TestDiscussionsConfig }
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models.UUIDGenerator
import org.specs2.mutable.Specification

final class ChannelWritesSpec extends Specification with IOTest with ResourcefulTest {

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  // populated by resources
  private var transactor:        Transactor[IO]        = null
  private var discussionsWrites: DiscussionsWrites[IO] = null

  override protected def testResource: Resource[IO, Unit] =
    for {
      domainCfg <- TestDiscussionsConfig.loadDomainConfig[IO]
      xa <- new PostgresDatabase(domainCfg.database).transactor[IO]
      writes <- DiscussionsModule.writes[IO](domainCfg)
    } yield {
      transactor        = xa
      discussionsWrites = writes
    }

  "Channel Writes" should {

    "have tests with working resource management" in {
      1 === 1
    }
  }
}
