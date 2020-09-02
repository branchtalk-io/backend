package io.branchtalk.discussions

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models.UUIDGenerator
import org.specs2.mutable.Specification

import scala.concurrent.duration._

final class ChannelReadsWritesSpec extends Specification with IOTest with ResourcefulTest with DiscussionsFixtures {

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  // populated by resources
  private var transactor:        Transactor[IO]        = _
  private var discussionsReads:  DiscussionsReads[IO]  = _
  private var discussionsWrites: DiscussionsWrites[IO] = _

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

    "create Channel and eventually read it" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          creationData <- (0 until 3).toList.traverse(_ => channelCreate)
          // when
          _ <- projector.handleError(error => error.printStackTrace()).start
          scheduled <- creationData.traverse(discussionsWrites.channelWrites.createChannel)
          ids = scheduled.map(_.id)
          channels <- ids.traverse(discussionsReads.channelReads.requireById).eventually()
          channelsOpt <- ids.traverse(discussionsReads.channelReads.getById).eventually()
          channelsExist <- ids.traverse(discussionsReads.channelReads.exists).eventually()
        } yield {
          // then
          ids.toSet === channels.map(_.id).toSet
          channelsOpt.forall(_.isDefined) must beTrue
          channelsExist.forall(identity) must beTrue
        }
      }
    }

    "allow delete and undelete of a created Channel" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          creationData <- (0 until 3).toList.traverse(_ => channelCreate)
          editorID <- editorIDCreate
          // when
          _ <- projector.handleError(error => error.printStackTrace()).start
          scheduled <- creationData.traverse(discussionsWrites.channelWrites.createChannel)
          ids = scheduled.map(_.id)
          _ <- ids.traverse(discussionsReads.channelReads.requireById).eventually()
          _ <- ids.map(Channel.Delete(_, editorID)).traverse(discussionsWrites.channelWrites.deleteChannel)
          _ <- ids
            .traverse(discussionsReads.channelReads.getById)
            .flatTap(results => IO(assert(results.forall(_.isEmpty), "All Channels should be eventually deleted")))
            .eventually()
          _ <- ids.map(Channel.Restore(_, editorID)).traverse(discussionsWrites.channelWrites.restoreChannel)
          restored <- ids
            .traverse(discussionsReads.channelReads.getById)
            .flatTap { results =>
              IO(assert(results.forall(_.isDefined), "All Channels should be eventually restored"))
            }
            .eventually()
          restoredIds = restored.flatten.map(_.id)
        } yield {
          // then
          ids.toSet === restoredIds.toSet
        }
      }
    }
  }
}
