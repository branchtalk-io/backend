package io.branchtalk.discussions.api

import cats.effect.IO
import io.branchtalk.api.{ Pagination, ServerIOTest }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.PostModels.APIPost
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.users.UsersFixtures
import org.specs2.mutable.Specification
import sttp.model.StatusCode

// NOTE: This test requires a running Postgres (Docker) to execute; it will compile but needs Docker for runtime.
final class SearchServerSpec extends Specification, ServerIOTest, UsersFixtures, DiscussionsFixtures {

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "SearchServer-provided endpoints" should {

    "on GET /discussions/search" in {

      "return Posts matching a full-text search query" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          // create a post with known content for searching
          creationData <- postCreate(channelID)
          CreationScheduled(postID) <- discussionsWrites.postWrites.createPost(creationData)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          // extract a word from the post title to use as search query
          searchQuery = creationData.title.unwrap.split("\\s+").headOption.getOrElse("test")
          // when
          response <- SearchAPIs.search.toTestCall.untupled(
            None,
            searchQuery,
            None,
            None,
            None
          )
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[Pagination[APIPost]]))
        }
      }

      "return Posts filtered by channelID" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          creationData <- postCreate(channelID)
          CreationScheduled(postID) <- discussionsWrites.postWrites.createPost(creationData)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          searchQuery = creationData.title.unwrap.split("\\s+").headOption.getOrElse("test")
          // when
          response <- SearchAPIs.search.toTestCall.untupled(
            None,
            searchQuery,
            Some(channelID),
            None,
            None
          )
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[Pagination[APIPost]]))
        }
      }

      "support pagination parameters" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          _ <- (0 until 5).toList.traverse(_ =>
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.unwrap)
          )
          // wait for all posts to be indexed
          _ <- IO.sleep(scala.concurrent.duration.FiniteDuration(1, "second"))
          // when - use a very common English word that should match lorem ipsum content
          response <- SearchAPIs.search.toTestCall.untupled(
            None,
            "lorem",
            Some(channelID),
            Pagination.Offset(0L).some,
            Pagination.Limit(2).some
          )
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[Pagination[APIPost]]))
        }
      }
    }
  }
}
