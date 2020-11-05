package io.branchtalk.discussions

import cats.effect.IO
import io.branchtalk.discussions.model.{ Comment, Post }
import io.branchtalk.shared.models.{ ID, TestUUIDGenerator, Updatable }
import org.specs2.mutable.Specification

final class CommentReadsWritesSpec extends Specification with DiscussionsIOTest with DiscussionsFixtures {

  protected implicit val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Comment Reads & Writes" should {

    "don't create a Comment if there is no Post for it" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          postID <- ID.create[IO, Post]
          creationData <- (0 until 3).toList.traverse(_ => commentCreate(postID))
          // when
          toCreate <- creationData.traverse(discussionsWrites.commentWrites.createComment(_).attempt)
        } yield {
          // then
          toCreate must contain(beLeft[Throwable]).foreach
        }
      }
    }

    "create a Comment and eventually read it" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postID <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => commentCreate(postID))
          // when
          toCreate <- creationData.traverse(discussionsWrites.commentWrites.createComment)
          ids = toCreate.map(_.id)
          comments <- ids.traverse(discussionsReads.commentReads.requireById).eventually()
          commentsOpt <- ids.traverse(discussionsReads.commentReads.getById).eventually()
          commentsExist <- ids.traverse(discussionsReads.commentReads.exists).eventually()
          commentDeleted <- ids.traverse(discussionsReads.commentReads.deleted).eventually()
        } yield {
          // then
          ids must containTheSameElementsAs(comments.map(_.id))
          commentsOpt must contain(beSome[Comment]).foreach
          commentsExist must contain(beTrue).foreach
          commentDeleted must not(contain(beTrue).atLeastOnce)
        }
      }
    }

    "don't update a Comment that doesn't exists" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postID <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
          editorID <- editorIDCreate
          creationData <- (0 until 3).toList.traverse(_ => commentCreate(postID))
          fakeUpdateData <- creationData.traverse { data =>
            ID.create[IO, Comment].map { id =>
              Comment.Update(
                id         = id,
                editorID   = editorID,
                newContent = Updatable.Set(data.content)
              )
            }
          }
          // when
          toUpdate <- fakeUpdateData.traverse(discussionsWrites.commentWrites.updateComment(_).attempt)
        } yield {
          // then
          toUpdate must contain(beLeft[Throwable]).foreach
        }
      }
    }

    "update an existing Comment" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postID <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          editorID <- editorIDCreate
          creationData <- (0 until 2).toList.traverse(_ => commentCreate(postID))
          toCreate <- creationData.traverse(discussionsWrites.commentWrites.createComment)
          ids = toCreate.map(_.id)
          created <- ids.traverse(discussionsReads.commentReads.requireById).eventually()
          updateData = created.zipWithIndex.collect {
            case (Comment(id, data), 0) =>
              Comment.Update(
                id         = id,
                editorID   = editorID,
                newContent = Updatable.Set(data.content)
              )
            case (Comment(id, _), 1) =>
              Comment.Update(
                id         = id,
                editorID   = editorID,
                newContent = Updatable.Keep
              )
          }
          // when
          _ <- updateData.traverse(discussionsWrites.commentWrites.updateComment)
          updated <- ids
            .traverse(discussionsReads.commentReads.requireById)
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
              case ((Comment(_, older), Comment(_, newer)), 0) =>
                // set case
                older must_=== newer.copy(lastModifiedAt = None)
              case ((Comment(_, older), Comment(_, newer)), 1) =>
                // keep case
                older must_=== newer
            }
            .lastOption
            .getOrElse(true must beFalse)
        }
      }
    }

    "allow delete and restore of a created Comment" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          channelID <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postID <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => commentCreate(postID))
          editorID <- editorIDCreate
          // when
          toCreate <- creationData.traverse(discussionsWrites.commentWrites.createComment)
          ids = toCreate.map(_.id)
          _ <- ids.traverse(discussionsReads.commentReads.requireById).eventually()
          _ <- ids.map(Comment.Delete(_, editorID)).traverse(discussionsWrites.commentWrites.deleteComment)
          _ <- ids
            .traverse(discussionsReads.commentReads.getById)
            .flatTap(results => IO(assert(results.forall(_.isEmpty), "All Comments should be eventually deleted")))
            .eventually()
          notExist <- ids.traverse(discussionsReads.commentReads.exists)
          areDeleted <- ids.traverse(discussionsReads.commentReads.deleted)
          _ <- ids.map(Comment.Restore(_, editorID)).traverse(discussionsWrites.commentWrites.restoreComment)
          toRestore <- ids
            .traverse(discussionsReads.commentReads.getById)
            .flatTap(results => IO(assert(results.forall(_.isDefined), "All Comments should be eventually restored")))
            .eventually()
          restoredIds = toRestore.flatten.map(_.id)
          areRestored <- ids.traverse(discussionsReads.commentReads.exists)
          notDeleted <- ids.traverse(discussionsReads.commentReads.deleted)
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
  }
}
