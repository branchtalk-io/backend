package io.branchtalk.discussions.api

import io.branchtalk.api.{ Pagination, ServerIOTest }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.CommentModels.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.users.UsersFixtures
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class CommentServerPaginationSpec extends Specification, ServerIOTest, UsersFixtures, DiscussionsFixtures {

  // Comment pagination tests filter by postID, so they can run in parallel with other tests
  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "CommentServer-provided pagination endpoints" should {

    "on GET /discussions/channels/{channelID}/posts/{postID}/comments/newest" in {

      "return paginated newest Comments" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          commentIDs <- (0 until 10).toList.traverse(_ =>
            commentCreate(postID).flatMap(discussionsWrites.commentWrites.createComment).map(_.unwrap)
          )
          comments <- commentIDs.traverse(discussionsReads.commentReads.requireById(_)).eventually()
          // when
          response1 <- CommentAPIs.newest.toTestCall.untupled(None,
                                                              channelID,
                                                              postID,
                                                              None,
                                                              Pagination.Limit(5).some,
                                                              None
          )
          response2 <- CommentAPIs.newest.toTestCall.untupled(None,
                                                              channelID,
                                                              postID,
                                                              Pagination.Offset(5L).some,
                                                              Pagination.Limit(5).some,
                                                              None
          )
        } yield {
          // then
          response1.code === StatusCode.Ok
          response1.body must beValid(beRight(beAnInstanceOf[Pagination[APIComment]]))
          response2.code === StatusCode.Ok
          response2.body must beValid(beRight(beAnInstanceOf[Pagination[APIComment]]))
          (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
            .mapN { (pagination1, pagination2) =>
              (pagination1.entities.toSet ++ pagination2.entities.toSet) === comments
                .map(APIComment.fromDomain)
                .toSet
            }
            .getOrElse(pass)
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/{postID}/comments/hottest" in {

      "return paginated hottest Comments" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          commentIDs <- (0 until 10).toList.traverse(_ =>
            commentCreate(postID).flatMap(discussionsWrites.commentWrites.createComment).map(_.unwrap)
          )
          comments <- commentIDs.traverse(discussionsReads.commentReads.requireById(_)).eventually()
          // when
          response <- CommentAPIs.hottest.toTestCall.untupled(None, channelID, postID, None)
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[Pagination[APIComment]]))
          response.body.toValidOpt
            .flatMap(_.toOption)
            .map(pagination => pagination.entities.toSet === comments.map(APIComment.fromDomain).toSet)
            .getOrElse(pass)
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/{postID}/comments/controversial" in {

      "return paginated controversial Comments" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          commentIDs <- (0 until 10).toList.traverse(_ =>
            commentCreate(postID).flatMap(discussionsWrites.commentWrites.createComment).map(_.unwrap)
          )
          comments <- commentIDs.traverse(discussionsReads.commentReads.requireById(_)).eventually()
          // when
          response <- CommentAPIs.controversial.toTestCall.untupled(None, channelID, postID, None)
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[Pagination[APIComment]]))
          response.body.toValidOpt
            .flatMap(_.toOption)
            .map(_.entities.toSet === comments.map(APIComment.fromDomain).toSet)
            .getOrElse(pass)
        }
      }
    }
  }
}
