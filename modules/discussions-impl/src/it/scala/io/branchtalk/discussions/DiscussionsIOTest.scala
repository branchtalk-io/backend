package io.branchtalk.discussions

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.shared.model.UUIDGenerator

trait DiscussionsIOTest extends IOTest with ResourcefulTest {

  implicit protected def uuidGenerator: UUIDGenerator

  // populated by resources
  protected var discussionsReads:  DiscussionsReads[IO]  = _
  protected var discussionsWrites: DiscussionsWrites[IO] = _

  protected lazy val discussionsResource: Resource[IO, Unit] = for {
    discussionsCfg <- TestDiscussionsConfig.loadDomainConfig[IO]
    _ <- DiscussionsModule.reads[IO](discussionsCfg, registry).map(discussionsReads = _)
    _ <- DiscussionsModule.writes[IO](discussionsCfg, registry).map(discussionsWrites = _)
  } yield ()

  override protected def testResource: Resource[IO, Unit] = super.testResource >> discussionsResource

  protected def withDiscussionsProjections[A](fa: IO[A]): IO[A] =
    discussionsWrites.runProjector.use { discussionsProjector =>
      for {
        _ <- discussionsProjector.logError("Error reported by Discussions projector").start
        a <- fa
      } yield a
    }
}
