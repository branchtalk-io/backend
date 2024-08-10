package io.branchtalk.discussions

import cats.effect.{ IO, Resource }
import io.branchtalk.shared.infrastructure.DomainModule
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.shared.model.*

trait DiscussionsIOTest extends IOTest, ResourcefulTest {

  protected given uuidGenerator: UUID.Generator

  // populated by resources
  protected var discussionsCfg:    DomainModule.Config   = _
  protected var discussionsReads:  DiscussionsReads[IO]  = _
  protected var discussionsWrites: DiscussionsWrites[IO] = _

  protected lazy val discussionsResource: Resource[IO, Unit] = for {
    _ <- TestDiscussionsConfig.loadDomainConfig[IO].map(discussionsCfg = _)
    _ <- DiscussionsModule.reads[IO](discussionsCfg, registry).map(discussionsReads = _)
    _ <- DiscussionsModule.writes[IO](discussionsCfg, registry).map(discussionsWrites = _)
    _ <- discussionsWrites.runProjecions.asResource
  } yield ()

  override protected def testResource: Resource[IO, Unit] = super.testResource >> discussionsResource
}
