package io.branchtalk.discussions

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models.UUIDGenerator
import org.specs2.mutable.Specification

import scala.concurrent.duration._

final class ChannelReadsWritesSpec extends Specification with IOTest with ResourcefulTest with DiscussionsFixtures {

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
        for {
          // given
          creationData <- (0 until 3).toList.traverse(_ => channelCreate)
          // when
          _ <- projector
            .handleError(error => error.printStackTrace())
            .flatTap(_ => IO(println("subscriber closed")))
            .start
          scheduled <- creationData.traverse(discussionsWrites.channelWrites.createChannel)
          _ <- IO.sleep(5.seconds)
          channels <- scheduled.map(_.id).traverse(discussionsReads.channelReads.requireById).eventually()
          channelsOpt <- scheduled.map(_.id).traverse(discussionsReads.channelReads.getById).eventually()
          channelsExist <- scheduled.map(_.id).traverse(discussionsReads.channelReads.exists).eventually()
        } yield {
          // then
          scheduled.map(_.id).toSet === channels.map(_.id).toSet
          channelsOpt.forall(_.isDefined) must beTrue
          channelsExist.forall(identity) must beTrue
        }
      }
    }
  }

  // TODO: test rest of Channels API
}
