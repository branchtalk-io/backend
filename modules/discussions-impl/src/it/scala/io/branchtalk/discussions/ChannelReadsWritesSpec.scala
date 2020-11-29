package io.branchtalk.discussions

import cats.effect.IO
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.{ ID, OptionUpdatable, TestUUIDGenerator, Updatable }
import org.specs2.mutable.Specification

final class ChannelReadsWritesSpec extends Specification with DiscussionsIOTest with DiscussionsFixtures {

  sequential // Channel pagination tests cannot be run in parallel to other Channel tests

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Channel Reads & Writes" should {

    "create a Channel and eventually read it" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          creationData <- (0 until 3).toList.traverse(_ => channelCreate)
          // when
          toCreate <- creationData.traverse(discussionsWrites.channelWrites.createChannel)
          ids = toCreate.map(_.id)
          channels <- ids.traverse(discussionsReads.channelReads.requireById(_)).eventually()
          channelsOpt <- ids.traverse(discussionsReads.channelReads.getById(_)).eventually()
          channelsExist <- ids.traverse(discussionsReads.channelReads.exists).eventually()
          channelDeleted <- ids.traverse(discussionsReads.channelReads.deleted).eventually()
        } yield {
          // then
          ids must containTheSameElementsAs(channels.map(_.id))
          channelsOpt must contain(beSome[Channel]).foreach
          channelsExist must contain(beTrue).foreach
          channelDeleted must not(contain(beTrue).atLeastOnce)
        }
      }
    }

    "don't update a Channel that doesn't exists" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          editorID <- editorIDCreate
          creationData <- (0 until 3).toList.traverse(_ => channelCreate)
          fakeUpdateData <- creationData.traverse { data =>
            ID.create[IO, Channel].map { id =>
              Channel.Update(
                id = id,
                editorID = editorID,
                newUrlName = Updatable.Set(data.urlName),
                newName = Updatable.Set(data.name),
                newDescription = OptionUpdatable.setFromOption(data.description)
              )
            }
          }
          // when
          toUpdate <- fakeUpdateData.traverse(discussionsWrites.channelWrites.updateChannel(_).attempt)
        } yield
        // then
        toUpdate must contain(beLeft[Throwable]).foreach
      }
    }

    "update an existing Channel" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          editorID <- editorIDCreate
          creationData <- (0 until 3).toList.traverse(_ => channelCreate)
          toCreate <- creationData.traverse(discussionsWrites.channelWrites.createChannel)
          ids = toCreate.map(_.id)
          created <- ids.traverse(discussionsReads.channelReads.requireById(_)).eventually()
          updateData = created.zipWithIndex.collect {
            case (Channel(id, data), 0) =>
              Channel.Update(
                id = id,
                editorID = editorID,
                newUrlName = Updatable.Set(data.urlName),
                newName = Updatable.Set(data.name),
                newDescription = OptionUpdatable.setFromOption(data.description)
              )
            case (Channel(id, _), 1) =>
              Channel.Update(
                id = id,
                editorID = editorID,
                newUrlName = Updatable.Keep,
                newName = Updatable.Keep,
                newDescription = OptionUpdatable.Keep
              )
            case (Channel(id, _), 2) =>
              Channel.Update(
                id = id,
                editorID = editorID,
                newUrlName = Updatable.Keep,
                newName = Updatable.Keep,
                newDescription = OptionUpdatable.Erase
              )
          }
          // when
          _ <- updateData.traverse(discussionsWrites.channelWrites.updateChannel)
          updated <- ids
            .traverse(discussionsReads.channelReads.requireById(_))
            .assert("Updated entity should have lastModifiedAt set")(_.last.data.lastModifiedAt.isDefined)
            .eventually()
        } yield
        // then
        created
          .zip(updated)
          .zipWithIndex
          .collect {
            case ((Channel(_, older), Channel(_, newer)), 0) =>
              // set case
              older must_=== newer.copy(lastModifiedAt = None)
            case ((Channel(_, older), Channel(_, newer)), 1) =>
              // keep case
              older must_=== newer
            case ((Channel(_, older), Channel(_, newer)), 2) =>
              // erase case
              older.copy(description = None) must_=== newer.copy(lastModifiedAt = None)
          }
          .lastOption
          .getOrElse(true must beFalse)
      }
    }

    "allow delete and restore of a created Channel" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          editorID <- editorIDCreate
          creationData <- (0 until 3).toList.traverse(_ => channelCreate)
          // when
          toCreate <- creationData.traverse(discussionsWrites.channelWrites.createChannel)
          ids = toCreate.map(_.id)
          _ <- ids.traverse(discussionsReads.channelReads.requireById(_)).eventually()
          _ <- ids.map(Channel.Delete(_, editorID)).traverse(discussionsWrites.channelWrites.deleteChannel)
          _ <- ids
            .traverse(discussionsReads.channelReads.getById(_))
            .assert("All Channels should be eventually deleted")(_.forall(_.isEmpty))
            .eventually()
          _ <- ids
            .traverse(discussionsReads.channelReads.getById(_, isDeleted = true))
            .assert("All Posts should be obtainable as getById with isDeleted=true")(_.forall(_.isDefined))
            .eventually()
          _ <- ids.traverse(discussionsReads.channelReads.requireById(_, isDeleted = true)).eventually()
          notExist <- ids.traverse(discussionsReads.channelReads.exists)
          areDeleted <- ids.traverse(discussionsReads.channelReads.deleted)
          _ <- ids.map(Channel.Restore(_, editorID)).traverse(discussionsWrites.channelWrites.restoreChannel)
          toRestore <- ids
            .traverse(discussionsReads.channelReads.getById(_))
            .assert("All Channels should be eventually restored")(_.forall(_.isDefined))
            .eventually()
          restoredIds = toRestore.flatten.map(_.id)
          areRestored <- ids.traverse(discussionsReads.channelReads.exists)
          notDeleted <- ids.traverse(discussionsReads.channelReads.deleted)
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

    "paginate newest Channels" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          editorID <- editorIDCreate
          cleanupIDs <- discussionsReads.channelReads.paginate(Channel.Sorting.Newest, 0L, 10).map(_.entities.map(_.id))
          _ <- cleanupIDs.traverse(id => discussionsWrites.channelWrites.deleteChannel(Channel.Delete(id, editorID)))
          _ <- cleanupIDs
            .traverse(discussionsReads.channelReads.deleted(_))
            .assert("Channels should be deleted eventually")(_.forall(identity))
            .eventually()
          paginatedData <- (0 until 20).toList.traverse(_ => channelCreate)
          paginatedIds <- paginatedData.traverse(discussionsWrites.channelWrites.createChannel).map(_.map(_.id))
          _ <- paginatedIds.traverse(discussionsReads.channelReads.requireById(_)).eventually()
          // when
          pagination <- discussionsReads.channelReads.paginate(Channel.Sorting.Newest, 0L, 10)
          pagination2 <- discussionsReads.channelReads.paginate(Channel.Sorting.Newest, 10L, 10)
        } yield {
          // then
          pagination.entities must haveSize(10)
          pagination.nextOffset.map(_.value) must beSome(10)
          pagination2.entities must haveSize(10)
          pagination2.nextOffset.map(_.value) must beNone
        }
      }
    }

    "paginate Channels alphabetically" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          editorID <- editorIDCreate
          cleanupIDs <- discussionsReads.channelReads
            .paginate(Channel.Sorting.Newest, 0L, 1000)
            .map(_.entities.map(_.id))
            .flatMap(_.traverse(id => discussionsWrites.channelWrites.deleteChannel(Channel.Delete(id, editorID))))
            .map(_.map(_.id))
          _ <- cleanupIDs
            .traverse(discussionsReads.channelReads.deleted(_))
            .assert("Channels should be deleted eventually")(_.forall(identity))
            .eventually()
          paginatedData <- (0 until 20).toList.traverse(_ => channelCreate)
          paginatedIds <- paginatedData.traverse(discussionsWrites.channelWrites.createChannel).map(_.map(_.id))
          _ <- paginatedIds.traverse(discussionsReads.channelReads.requireById(_)).eventually()
          // when
          pagination <- discussionsReads.channelReads.paginate(Channel.Sorting.Alphabetically, 0L, 10)
          pagination2 <- discussionsReads.channelReads.paginate(Channel.Sorting.Alphabetically, 10L, 10)
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
