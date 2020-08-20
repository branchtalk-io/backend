package io.branchtalk.discussions

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models.UUIDGenerator
import org.specs2.mutable.Specification

final class ChannelReadsWritesSpec extends Specification with IOTest with ResourcefulTest {

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  // populated by resources
  private var transactor:        Transactor[IO]        = null
  private var discussionsReads:  DiscussionsReads[IO]  = null
  private var discussionsWrites: DiscussionsWrites[IO] = null

  override protected def testResource: Resource[IO, Unit] =
    for {
      domainCfg <- TestDiscussionsConfig.loadDomainConfig[IO]
      xa <- new PostgresDatabase(domainCfg.database).transactor[IO]
      reads <- DiscussionsModule.reads[IO](domainCfg)
      writes <- DiscussionsModule.writes[IO](domainCfg)
    } yield {
      transactor        = xa
      discussionsReads  = reads
      discussionsWrites = writes
    }

  "Channel Reads & Writes" should {

    "create Channel and read it" in {
      discussionsWrites.runProjector.use { projector =>
        // given
        val creationData = List.empty[Channel.Create]

        // when
        for {
          _ <- projector.start
          scheduled <- creationData.traverse(discussionsWrites.channelWrites.createChannel)
          channels <- scheduled.map(_.id).traverse(discussionsReads.channelReads.requireById).eventually()
        } yield (scheduled.map(_.id).toSet === channels.map(_.id).toSet)
      }
    }
  }

  // TODO: test rest of Channels API
}
