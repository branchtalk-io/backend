package io.branchtalk.discussions

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.discussions.model.Post
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.models.UUIDGenerator
import org.specs2.mutable.Specification

final class PostReadsWritesSpec extends Specification with IOTest with ResourcefulTest with DiscussionsFixtures {

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  // populated by resources
  //private var transactor:        Transactor[IO]        = _
  private var discussionsReads:  DiscussionsReads[IO]  = _
  private var discussionsWrites: DiscussionsWrites[IO] = _

  override protected def testResource: Resource[IO, Unit] =
    for {
      domainCfg <- TestDiscussionsConfig.loadDomainConfig[IO]
      _ <- new PostgresDatabase(domainCfg.database).transactor[IO]
      reads <- DiscussionsModule.reads[IO](domainCfg)
      writes <- DiscussionsModule.writes[IO](domainCfg)
    } yield {
      //transactor        = xa
      discussionsReads  = reads
      discussionsWrites = writes
    }

  "Post Reads & Writes" should {

    // TODO: prevent creation of a post without a channel

    "create a Post and eventually read it" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => postCreate(channelID))
          // when
          toCreate <- creationData.traverse(discussionsWrites.postWrites.createPost)
          ids = toCreate.map(_.id)
          posts <- ids.traverse(discussionsReads.postReads.requireById).eventually()
          postsOpt <- ids.traverse(discussionsReads.postReads.getById).eventually()
          postsExist <- ids.traverse(discussionsReads.postReads.exists).eventually()
          postDeleted <- ids.traverse(discussionsReads.postReads.deleted).eventually()
        } yield {
          // then
          ids.toSet === posts.map(_.id).toSet
          postsOpt.forall(_.isDefined) must beTrue
          postsExist.forall(identity) must beTrue
          postDeleted.exists(identity) must beFalse
        }
      }
    }

    "allow delete and restore of a created Post" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => postCreate(channelID))
          editorID <- editorIDCreate
          // when
          toCreate <- creationData.traverse(discussionsWrites.postWrites.createPost)
          ids = toCreate.map(_.id)
          _ <- ids.traverse(discussionsReads.postReads.requireById).eventually()
          _ <- ids.map(Post.Delete(_, editorID)).traverse(discussionsWrites.postWrites.deletePost)
          _ <- ids
            .traverse(discussionsReads.postReads.getById)
            .flatTap(results => IO(assert(results.forall(_.isEmpty), "All Posts should be eventually deleted")))
            .eventually()
          notExist <- ids.traverse(discussionsReads.postReads.exists)
          areDeleted <- ids.traverse(discussionsReads.postReads.deleted)
          _ <- ids.map(Post.Restore(_, editorID)).traverse(discussionsWrites.postWrites.restorePost)
          toRestore <- ids
            .traverse(discussionsReads.postReads.getById)
            .flatTap(results => IO(assert(results.forall(_.isDefined), "All Posts should be eventually restored")))
            .eventually()
          restoredIds = toRestore.flatten.map(_.id)
          areRestored <- ids.traverse(discussionsReads.postReads.exists)
          notDeleted <- ids.traverse(discussionsReads.postReads.deleted)
        } yield {
          // then
          ids.toSet === restoredIds.toSet
          notExist.exists(identity) must beFalse
          areDeleted.forall(identity) must beTrue
          areRestored.forall(identity) must beTrue
          notDeleted.exists(identity) must beFalse
        }
      }

      // TODO: test update
    }
  }
}
