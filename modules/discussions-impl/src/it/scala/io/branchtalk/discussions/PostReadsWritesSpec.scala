package io.branchtalk.discussions

import cats.data.NonEmptySet
import cats.effect.IO
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.model.{ ID, TestUUIDGenerator, Updatable }
import org.specs2.mutable.Specification

final class PostReadsWritesSpec extends Specification with DiscussionsIOTest with DiscussionsFixtures {

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Post Reads & Writes" should {

    "don't create a Post if there is no Channel for it" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- ID.create[IO, Channel]
          creationData <- (0 until 3).toList.traverse(_ => postCreate(channelID))
          // when
          toCreate <- creationData.traverse(discussionsWrites.postWrites.createPost(_).attempt)
        } yield
        // then
        toCreate must contain(beLeft[Throwable]).foreach
      }
    }

    "create a Post and eventually read it" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => postCreate(channelID))
          // when
          toCreate <- creationData.traverse(discussionsWrites.postWrites.createPost)
          ids = toCreate.map(_.id)
          posts <- ids.traverse(discussionsReads.postReads.requireById(_)).eventually()
          postsOpt <- ids.traverse(discussionsReads.postReads.getById(_)).eventually()
          postsExist <- ids.traverse(discussionsReads.postReads.exists).eventually()
          postDeleted <- ids.traverse(discussionsReads.postReads.deleted).eventually()
        } yield {
          // then
          ids must containTheSameElementsAs(posts.map(_.id))
          postsOpt must contain(beSome[Post]).foreach
          postsExist must contain(beTrue).foreach
          postDeleted must not(contain(beTrue).atLeastOnce)
        }
      }
    }

    "don't update a Post that doesn't exists" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          editorID <- editorIDCreate
          creationData <- (0 until 3).toList.traverse(_ => postCreate(channelID))
          fakeUpdateData <- creationData.traverse { data =>
            ID.create[IO, Post].map { id =>
              Post.Update(
                id = id,
                editorID = editorID,
                newTitle = Updatable.Set(data.title),
                newContent = Updatable.Set(data.content)
              )
            }
          }
          // when
          toUpdate <- fakeUpdateData.traverse(discussionsWrites.postWrites.updatePost(_).attempt)
        } yield
        // then
        toUpdate must contain(beLeft[Throwable]).foreach
      }
    }

    "update an existing Post" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          editorID <- editorIDCreate
          creationData <- (0 until 2).toList.traverse(_ => postCreate(channelID))
          toCreate <- creationData.traverse(discussionsWrites.postWrites.createPost)
          ids = toCreate.map(_.id)
          created <- ids.traverse(discussionsReads.postReads.requireById(_)).eventually()
          updateData = created.zipWithIndex.collect {
            case (Post(id, data), 0) =>
              Post.Update(
                id = id,
                editorID = editorID,
                newTitle = Updatable.Set(data.title),
                newContent = Updatable.Set(data.content)
              )
            case (Post(id, _), 1) =>
              Post.Update(
                id = id,
                editorID = editorID,
                newTitle = Updatable.Keep,
                newContent = Updatable.Keep
              )
          }
          // when
          _ <- updateData.traverse(discussionsWrites.postWrites.updatePost)
          updated <- ids
            .traverse(discussionsReads.postReads.requireById(_))
            .assert("Updated entity should have lastModifiedAt set")(_.head.data.lastModifiedAt.isDefined)
            .eventually()
        } yield
        // then
        created
          .zip(updated)
          .zipWithIndex
          .collect {
            case ((Post(_, older), Post(_, newer)), 0) =>
              // set case
              older must_=== newer.copy(lastModifiedAt = None)
            case ((Post(_, older), Post(_, newer)), 1) =>
              // keep case
              older must_=== newer
          }
          .lastOption
          .getOrElse(true must beFalse)
      }
    }

    "allow delete and restore of a created Post" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => postCreate(channelID))
          editorID <- editorIDCreate
          // when
          toCreate <- creationData.traverse(discussionsWrites.postWrites.createPost)
          ids = toCreate.map(_.id)
          _ <- ids.traverse(discussionsReads.postReads.requireById(_)).eventually()
          _ <- ids.map(Post.Delete(_, editorID)).traverse(discussionsWrites.postWrites.deletePost)
          _ <- ids
            .traverse(discussionsReads.postReads.getById(_))
            .assert("All Posts should be eventually deleted")(_.forall(_.isEmpty))
            .eventually()
          _ <- ids
            .traverse(discussionsReads.postReads.getById(_, isDeleted = true))
            .assert("All Posts should be obtainable as getById with isDeleted=true")(_.forall(_.isDefined))
            .eventually()
          _ <- ids.traverse(discussionsReads.postReads.requireById(_, isDeleted = true)).eventually()
          notExist <- ids.traverse(discussionsReads.postReads.exists)
          areDeleted <- ids.traverse(discussionsReads.postReads.deleted)
          _ <- ids.map(Post.Restore(_, editorID)).traverse(discussionsWrites.postWrites.restorePost)
          toRestore <- ids
            .traverse(discussionsReads.postReads.getById(_))
            .assert("All Posts should be eventually restored")(_.forall(_.isDefined))
            .eventually()
          restoredIds = toRestore.flatten.map(_.id)
          areRestored <- ids.traverse(discussionsReads.postReads.exists)
          notDeleted <- ids.traverse(discussionsReads.postReads.deleted)
        } yield {
          // then
          ids must containTheSameElementsAs(restoredIds)
          notExist must contain(beFalse).foreach
          areDeleted must contain(beTrue).foreach
          areRestored must contain(beTrue).foreach
          notDeleted must contain(beFalse).foreach
        }
      }
    }

    "paginate newest Posts by Channels" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          channel2ID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channel2ID).eventually()
          // editorID <- editorIDCreate
          paginatedData <- (0 until 20).toList.traverse(_ => postCreate(channelID))
          paginatedIds <- paginatedData.traverse(discussionsWrites.postWrites.createPost).map(_.map(_.id))
          nonPaginatedData <- (0 until 20).toList.traverse(_ => postCreate(channel2ID))
          nonPaginatedIds <- nonPaginatedData.traverse(discussionsWrites.postWrites.createPost).map(_.map(_.id))
          _ <- (paginatedIds ++ nonPaginatedIds).traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          pagination <- discussionsReads.postReads.paginate(NonEmptySet.of(channelID), 0L, 10)
          pagination2 <- discussionsReads.postReads.paginate(NonEmptySet.of(channelID), 10L, 10)
        } yield {
          // then
          pagination.entities must haveSize(10)
          pagination.nextOffset.map(_.value) must beSome(10)
          pagination2.entities must haveSize(10)
          pagination2.nextOffset.map(_.value) must beNone
        }
      }
    }
  }
}
