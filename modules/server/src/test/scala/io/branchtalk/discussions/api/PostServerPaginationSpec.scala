package io.branchtalk.discussions.api

import io.branchtalk.api.{ Pagination, ServerIOTest }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.PostModels.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.users.UsersFixtures
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class PostServerPaginationSpec extends Specification, ServerIOTest, UsersFixtures, DiscussionsFixtures {

  // Post pagination tests filter by channelID, so they can run in parallel with other tests
  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "PostServer-provided pagination endpoints" should {

    "on GET /discussions/channels/{channelID}/posts/newest" in {

      "return paginated newest Posts" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postIDs <- (0 until 10).toList.traverse(_ =>
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.unwrap)
          )
          posts <- postIDs.traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          response1 <- PostAPIs.newest.toTestCall.untupled(None, channelID, None, Pagination.Limit(5).some)
          response2 <- PostAPIs.newest.toTestCall.untupled(None,
                                                           channelID,
                                                           Pagination.Offset(5L).some,
                                                           Pagination.Limit(5).some
          )
        } yield {
          // then
          response1.code === StatusCode.Ok
          response1.body must beValid(beRight(beAnInstanceOf[Pagination[APIPost]]))
          response2.code === StatusCode.Ok
          response2.body must beValid(beRight(beAnInstanceOf[Pagination[APIPost]]))
          (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
            .mapN { (pagination1, pagination2) =>
              (pagination1.entities.toSet ++ pagination2.entities.toSet) === posts.map(APIPost.fromDomain).toSet
            }
            .getOrElse(pass)
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/hottest" in {

      "return paginated hottest Posts" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postIDs <- (0 until 10).toList.traverse(_ =>
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.unwrap)
          )
          posts <- postIDs.traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          response <- PostAPIs.hottest.toTestCall.untupled(None, channelID)
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[Pagination[APIPost]]))
          response.body.toValidOpt
            .flatMap(_.toOption)
            .map(pagination => pagination.entities.toSet === posts.map(APIPost.fromDomain).toSet)
            .getOrElse(pass)
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/controversial" in {

      "return paginated controversial Posts" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postIDs <- (0 until 10).toList.traverse(_ =>
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.unwrap)
          )
          posts <- postIDs.traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          response <- PostAPIs.controversial.toTestCall.untupled(None, channelID)
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[Pagination[APIPost]]))
          response.body.toValidOpt
            .flatMap(_.toOption)
            .map(_.entities.toSet === posts.map(APIPost.fromDomain).toSet)
            .getOrElse(pass)
        }
      }
    }
  }
}
