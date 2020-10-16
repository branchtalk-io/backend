package io.branchtalk.discussions

import cats.data.NonEmptySet
import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.models.{ ID, UUIDGenerator, Updatable }
import org.specs2.mutable.Specification

final class PostReadsWritesSpec extends Specification with IOTest with ResourcefulTest with DiscussionsFixtures {

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  // populated by resources
  private var discussionsReads:  DiscussionsReads[IO]  = _
  private var discussionsWrites: DiscussionsWrites[IO] = _

  override protected def testResource: Resource[IO, Unit] =
    for {
      domainCfg <- TestDiscussionsConfig.loadDomainConfig[IO]
      reads <- DiscussionsModule.reads[IO](domainCfg)
      writes <- DiscussionsModule.writes[IO](domainCfg)
    } yield {
      discussionsReads  = reads
      discussionsWrites = writes
    }

  "Post Reads & Writes" should {

    "don't create a Post if there is no Channel for it" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.logError("Error reported by projector").start
          channelID <- ID.create[IO, Channel]
          creationData <- (0 until 3).toList.traverse(_ => postCreate(channelID))
          // when
          toCreate <- creationData.traverse(discussionsWrites.postWrites.createPost(_).attempt)
        } yield {
          // then
          toCreate.forall(_.isLeft) must beTrue
        }
      }
    }

    "create a Post and eventually read it" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.logError("Error reported by projector").start
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

    "don't update a Post that doesn't exists" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.logError("Error reported by projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          editorID <- editorIDCreate
          creationData <- (0 until 3).toList.traverse(_ => postCreate(channelID))
          fakeUpdateData <- creationData.traverse { data =>
            ID.create[IO, Post].map { id =>
              Post.Update(
                id         = id,
                editorID   = editorID,
                newTitle   = Updatable.Set(data.title),
                newContent = Updatable.Set(data.content)
              )
            }
          }
          // when
          toUpdate <- fakeUpdateData.traverse(discussionsWrites.postWrites.updatePost(_).attempt)
        } yield {
          // then
          toUpdate.forall(_.isLeft) must beTrue
        }
      }
    }

    "update an existing Post" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.logError("Error reported by projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          editorID <- editorIDCreate
          creationData <- (0 until 2).toList.traverse(_ => postCreate(channelID))
          toCreate <- creationData.traverse(discussionsWrites.postWrites.createPost)
          ids = toCreate.map(_.id)
          created <- ids.traverse(discussionsReads.postReads.requireById).eventually()
          updateData = created.zipWithIndex.collect {
            case (Post(id, data), 0) =>
              Post.Update(
                id         = id,
                editorID   = editorID,
                newTitle   = Updatable.Set(data.title),
                newContent = Updatable.Set(data.content)
              )
            case (Post(id, _), 1) =>
              Post.Update(
                id         = id,
                editorID   = editorID,
                newTitle   = Updatable.Keep,
                newContent = Updatable.Keep
              )
          }
          // when
          _ <- updateData.traverse(discussionsWrites.postWrites.updatePost)
          updated <- ids
            .traverse(discussionsReads.postReads.requireById)
            .flatTap { current =>
              IO(assert(current.head.data.lastModifiedAt.isDefined, "Updated entity should have lastModifiedAt set"))
            }
            .eventually()
        } yield {
          // then
          created
            .zip(updated)
            .zipWithIndex
            .collect {
              case ((Post(_, older), Post(_, newer)), 0) =>
                // set case
                older === newer.copy(lastModifiedAt = None)
              case ((Post(_, older), Post(_, newer)), 1) =>
                // keep case
                older === newer
            }
            .forall(identity) must beTrue
        }
      }
    }

    "allow delete and restore of a created Post" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.logError("Error reported by projector").start
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
    }

    "paginate newest Posts by Channels" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.logError("Error reported by projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          channel2ID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channel2ID).eventually()
          // editorID <- editorIDCreate
          paginatedData <- (0 until 20).toList.traverse(_ => postCreate(channelID))
          paginatedIds <- paginatedData.traverse(discussionsWrites.postWrites.createPost).map(_.map(_.id))
          nonPaginatedData <- (0 until 20).toList.traverse(_ => postCreate(channel2ID))
          nonPaginatedIds <- nonPaginatedData.traverse(discussionsWrites.postWrites.createPost).map(_.map(_.id))
          _ <- (paginatedIds ++ nonPaginatedIds).traverse(discussionsReads.postReads.requireById).eventually()
          // when
          pagination <- discussionsReads.postReads.paginate(NonEmptySet.of(channelID), 0L, 10)
          pagination2 <- discussionsReads.postReads.paginate(NonEmptySet.of(channelID), 10L, 10)
        } yield {
          // then
          pagination.entities.size must_=== 10
          pagination.nextOffset.map(_.value) must beSome(10)
          pagination2.entities.size must_=== 10
          pagination2.nextOffset.map(_.value) must beNone
        }
      }
    }
  }
}
