package io.branchtalk.discussions.api

import cats.effect.IO
import io.branchtalk.api.{ Authentication, Pagination, PaginationLimit, PaginationOffset, ServerIOTest }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.Post
import io.branchtalk.mappings._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import io.scalaland.chimney.dsl._
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class PostServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  implicit protected lazy val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "PostServer-provided endpoints" should {

    "on GET /discussions/channels/{channelID}/posts/newest" in {

      "return newest Posts for a specified Channels" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postIDs <- (0 until 10).toList.traverse(_ =>
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
          )
          posts <- postIDs.traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          response1 <- PostAPIs.newest.toTestCall.untupled(None, channelID, None, PaginationLimit(5).some)
          response2 <- PostAPIs.newest.toTestCall.untupled(None,
                                                           channelID,
                                                           PaginationOffset(5L).some,
                                                           PaginationLimit(5).some
          )
        } yield {
          // then
          response1.code must_=== StatusCode.Ok
          response1.body must beValid(beRight(anInstanceOf[Pagination[APIPost]]))
          response2.code must_=== StatusCode.Ok
          response2.body must beValid(beRight(anInstanceOf[Pagination[APIPost]]))
          (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
            .mapN { (pagination1, pagination2) =>
              (pagination1.entities.toSet ++ pagination2.entities.toSet) must_=== posts.map(APIPost.fromDomain).toSet
            }
            .getOrElse(pass)
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/hottest" in {

      "return hottest Posts for a specified Channels" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postIDs <- (0 until 10).toList.traverse(_ =>
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
          )
          posts <- postIDs.traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          response <- PostAPIs.hottest.toTestCall.untupled(None, channelID)
        } yield {
          // then
          response.code must_=== StatusCode.Ok
          response.body must beValid(beRight(anInstanceOf[Pagination[APIPost]]))
          response.body.toValidOpt
            .flatMap(_.toOption)
            .map(pagination => pagination.entities.toSet must_=== posts.map(APIPost.fromDomain).toSet)
            .getOrElse(pass)
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/controversial" in {

      "return controversial Posts for a specified Channels" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postIDs <- (0 until 10).toList.traverse(_ =>
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
          )
          posts <- postIDs.traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          response <- PostAPIs.controversial.toTestCall.untupled(None, channelID)
        } yield {
          // then
          response.code must_=== StatusCode.Ok
          response.body must beValid(beRight(anInstanceOf[Pagination[APIPost]]))
          response.body.toValidOpt
            .flatMap(_.toOption)
            .map(_.entities.toSet must_=== posts.map(APIPost.fromDomain).toSet)
            .getOrElse(pass)
        }
      }
    }

    "on POST /discussions/channels/{channelID}/posts" in {

      "create a new Post" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          creationData <- postCreate(channelID)
          // when
          response <- PostAPIs.create.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID,
            creationData.transformInto[CreatePostRequest]
          )
          // TODO: check that this creates a new post eventually!
        } yield {
          // then
          response.code must_=== StatusCode.Ok
          response.body must beValid(beRight(anInstanceOf[CreatePostResponse]))
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/{postID}" in {

      "fetch existing Post" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
          post <- discussionsReads.postReads.requireById(postID).eventually()
          // when
          response <- PostAPIs.read.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)).some,
            channelID,
            postID
          )
        } yield {
          // then
          response.code must_=== StatusCode.Ok
          response.body must beValid(beRight(be_===(APIPost.fromDomain(post))))
        }
      }
    }

    "on PUT /discussions/channels/{channelID}/posts/{postID}" in {

      "update existing Post when User is its Author" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID)
            .map(_.focus(_.authorID).replace(userIDUsers2Discussions.get(userID))) // to own the Post
            .flatMap(discussionsWrites.postWrites.createPost)
          post <- discussionsReads.postReads.requireById(postID).eventually()
          newTitle <- Post.Title.parse[IO]("new title")
          newContent = Post.Content.Text(Post.Text("lorem ipsum"))
          // when
          response <- PostAPIs.update.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID,
            postID,
            UpdatePostRequest(
              newTitle = Updatable.Set(newTitle),
              newContent = Updatable.Set(newContent)
            )
          )
          updatedPost <- discussionsReads.postReads
            .requireById(postID)
            .assert("Updated entity should have lastModifiedAt set")(_.data.lastModifiedAt.isDefined)
            .eventually()
        } yield {
          // then
          response.code must_=== StatusCode.Ok
          response.body must beValid(beRight(be_===(UpdatePostResponse(postID))))
          updatedPost must_=== post
            .focus(_.data.title)
            .replace(newTitle)
            .focus(_.data.content)
            .replace(newContent)
            .focus(_.data.urlTitle)
            .replace(Post.UrlTitle("new-title"))
            .focus(_.data.lastModifiedAt)
            .replace(updatedPost.data.lastModifiedAt)
        }
      }
    }

    "on DELETE /discussions/channels/{channelID}/posts/{postID}" in {

      "delete existing Post when User is its Author" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID)
            .map(_.focus(_.authorID).replace(userIDUsers2Discussions.get(userID))) // to own the Post
            .flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          // when
          response <- PostAPIs.delete.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID,
            postID
          )
          _ <- discussionsReads.postReads
            .deleted(postID)
            .assert("Post should be eventually deleted")(identity)
            .eventually()
        } yield {
          // then
          response.code must_=== StatusCode.Ok
          response.body must beValid(beRight(be_===(DeletePostResponse(postID))))
        }
      }
    }

    "on POST /discussions/channels/{channelID}/posts/{postID}/restore" in {

      "restore deleted Post when User is its Author" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID)
            .map(_.focus(_.authorID).replace(userIDUsers2Discussions.get(userID))) // to own the Post
            .flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          _ <- discussionsWrites.postWrites.deletePost(Post.Delete(postID, userIDUsers2Discussions.get(userID)))
          _ <- discussionsReads.postReads.requireById(postID, isDeleted = true).eventually()
          // when
          response <- PostAPIs.restore.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID,
            postID
          )
          _ <- discussionsReads.postReads
            .exists(postID)
            .assert("Post should be eventually restored")(identity)
            .eventually()
        } yield {
          // then
          response.code must_=== StatusCode.Ok
          response.body must beValid(beRight(be_===(RestorePostResponse(postID))))
        }
      }
    }

    "on PUT /discussions/channels/{channelID}/posts/{postID}/upvote" in {

      "upvotes existing Post" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          // when
          response <- PostAPIs.upvote.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID,
            postID
          )
          _ <- discussionsReads.postReads
            .requireById(postID)
            .assert("Upvoted entity should have changed score")(_.data.totalScore.toInt =!= 0)
            .eventually()
        } yield
        // then
        response.code must_=== StatusCode.Ok
      }
    }

    "on PUT /discussions/channels/{channelID}/posts/{postID}/downvote" in {

      "downvotes existing Post" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          // when
          response <- PostAPIs.downvote.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID,
            postID
          )
          _ <- discussionsReads.postReads
            .requireById(postID)
            .assert("Downvoted entity should have changed score")(_.data.totalScore.toInt =!= 0)
            .eventually()
        } yield
        // then
        response.code must_=== StatusCode.Ok
      }
    }

    "on PUT /discussions/channels/{channelID}/posts/{postID}/revoke-vote" in {

      "revoke vote for existing Post" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()

          _ <- discussionsWrites.postWrites.upvotePost(Post.Upvote(postID, userIDUsers2Discussions.get(userID)))
          _ <- discussionsReads.postReads
            .requireById(postID)
            .assert("Upvoted entity should have changed score")(_.data.totalScore.toInt =!= 0)
            .eventually()
          // when
          response <- PostAPIs.revokeVote.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID,
            postID
          )
          _ <- discussionsReads.postReads
            .requireById(postID)
            .assert("Revoked-vote entity should have changed score")(_.data.totalScore.toInt === 0)
            .eventually()
        } yield
        // then
        response.code must_=== StatusCode.Ok
      }
    }
  }
}
