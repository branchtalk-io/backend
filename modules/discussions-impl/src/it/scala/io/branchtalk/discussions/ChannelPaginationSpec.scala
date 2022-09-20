package io.branchtalk.discussions

import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.TestUUIDGenerator
import org.specs2.mutable.Specification

final class ChannelPaginationSpec extends Specification with DiscussionsIOTest with DiscussionsFixtures {

  // Channel pagination tests cannot be run in parallel to other Channel tests (no parent to filter other tests)
  sequential

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Channel pagination" should {

    "paginate newest Channels" in {
      for {
        // given
        editorID <- editorIDCreate
        cleanupIDs <- discussionsReads.channelReads.paginate(Channel.Sorting.Newest, 0L, 10).map(_.entities.map(_.id))
        _ <- cleanupIDs.traverse(id => discussionsWrites.channelWrites.deleteChannel(Channel.Delete(id, editorID)))
        _ <- cleanupIDs
          .traverse(discussionsReads.channelReads.deleted(_))
          .assert("Channels should be deleted eventually")(_.forall(identity))
          .eventually()
        paginatedData <- (0 until 20).toList.traverse(_ => channelCreate)
        paginatedIDs <- paginatedData.traverse(discussionsWrites.channelWrites.createChannel).map(_.map(_.id))
        _ <- paginatedIDs.traverse(discussionsReads.channelReads.requireById(_)).eventually()
        // when
        pagination <- discussionsReads.channelReads.paginate(Channel.Sorting.Newest, 0L, 10)
        pagination2 <- discussionsReads.channelReads.paginate(Channel.Sorting.Newest, 10L, 10)
      } yield {
        // then
        pagination.entities must haveSize(10)
        pagination.nextOffset.map(_.value) must beSome(10L)
        pagination2.entities must haveSize(10)
        pagination2.nextOffset.map(_.value) must beNone
      }
    }

    "paginate Channels alphabetically" in {
      for {
        // given
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
        paginatedIDs <- paginatedData.traverse(discussionsWrites.channelWrites.createChannel).map(_.map(_.id))
        _ <- paginatedIDs.traverse(discussionsReads.channelReads.requireById(_)).eventually()
        // when
        pagination <- discussionsReads.channelReads.paginate(Channel.Sorting.Alphabetically, 0L, 10)
        pagination2 <- discussionsReads.channelReads.paginate(Channel.Sorting.Alphabetically, 10L, 10)
      } yield {
        // then
        pagination.entities must haveSize(10)
        pagination.nextOffset.map(_.value) must beSome(10L)
        pagination2.entities must haveSize(10)
        pagination2.nextOffset.map(_.value) must beNone
      }
    }
  }
}
