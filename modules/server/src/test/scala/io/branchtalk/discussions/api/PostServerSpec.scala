package io.branchtalk.discussions.api

import cats.effect.IO
import com.softwaremill.quicklens.*
import io.branchtalk.api.{ Authentication, Pagination, ServerIOTest, SessionID }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.PostModels.*
import io.branchtalk.discussions.model.Post
import io.branchtalk.mappings.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.users.UsersFixtures
import io.scalaland.chimney.dsl.*
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class PostServerSpec extends Specification, ServerIOTest, UsersFixtures, DiscussionsFixtures {

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "PostServer-provided endpoints" should {

    "on GET /discussions/channels/{channelID}/posts/newest" in {

      "return newest Posts for a specified Channels" in {
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

      "return hottest Posts for a specified Channels" in {
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

      "return controversial Posts for a specified Channels" in {
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
          createdPostID = response.body.toValidOpt.flatMap(_.toOption).map(_.id)
          _ <- createdPostID.traverse(postID =>
            discussionsReads.postReads
              .requireById(postID)
              .assert("Created Post should eventually appear on the read side")(_.data.title eqv creationData.title)
              .eventually()
          )
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[CreatePostResponse]))
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
          response.code === StatusCode.Ok
          response.body must beValid(beRight(be_==(APIPost.fromDomain(post))))
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
            .map(_.modify(_.authorID).setTo(userIDUsers2Discussions.get(userID))) // to own the Post
            .flatMap(discussionsWrites.postWrites.createPost)
          post <- discussionsReads.postReads.requireById(postID).eventually()
          newTitle <- ParseNewtype[IO].parse[Post.Title]("new title")
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
          response.code === StatusCode.Ok
          response.body must beValid(beRight(be_==(UpdatePostResponse(postID))))
          updatedPost === post
            .modify(_.data.title)
            .setTo(newTitle)
            .modify(_.data.content)
            .setTo(newContent)
            .modify(_.data.urlTitle)
            .setTo(Post.UrlTitle("new-title"))
            .modify(_.data.lastModifiedAt)
            .setTo(updatedPost.data.lastModifiedAt)
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
            .map(_.modify(_.authorID).setTo(userIDUsers2Discussions.get(userID))) // to own the Post
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
          response.code === StatusCode.Ok
          response.body must beValid(beRight(be_==(DeletePostResponse(postID))))
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
            .map(_.modify(_.authorID).setTo(userIDUsers2Discussions.get(userID))) // to own the Post
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
          response.code === StatusCode.Ok
          response.body must beValid(beRight(be_==(RestorePostResponse(postID))))
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
            .assert("Upvoted entity should have changed score")(_.data.totalScore.unwrap =!= 0)
            .eventually()
        } yield
        // then
        response.code === StatusCode.Ok
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
            .assert("Downvoted entity should have changed score")(_.data.totalScore.unwrap =!= 0)
            .eventually()
        } yield
        // then
        response.code === StatusCode.Ok
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
            .assert("Upvoted entity should have changed score")(_.data.totalScore.unwrap =!= 0)
            .eventually()
          // when
          response <- PostAPIs.revokeVote.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
            channelID,
            postID
          )
          _ <- discussionsReads.postReads
            .requireById(postID)
            .assert("Revoked-vote entity should have changed score")(_.data.totalScore.unwrap eqv 0)
            .eventually()
        } yield
        // then
        response.code === StatusCode.Ok
      }
    }

    // Negative-path tests

    "on POST /discussions/channels/{channelID}/posts with invalid session" in {

      "return 401 Unauthorized for a non-existent session" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          creationData <- postCreate(channelID)
          fakeSessionID = SessionID(java.util.UUID.randomUUID())
          // when
          response <- PostAPIs.create.toTestCall.untupled(
            Authentication.Session(sessionID = fakeSessionID),
            channelID,
            creationData.transformInto[CreatePostRequest]
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[PostError.BadCredentials]))
        }
      }
    }

    "on PUT /discussions/channels/{channelID}/posts/{postID} by non-owner" in {

      "return 401 when User is not the Post's Author and has no moderator permission" in {
        for {
          // given
          (CreationScheduled(ownerID), CreationScheduled(_)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(ownerID).eventually()
          (CreationScheduled(otherUserID), CreationScheduled(otherSessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(otherUserID).eventually()
          _ <- usersReads.sessionReads.requireById(otherSessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID)
            .map(_.modify(_.authorID).setTo(userIDUsers2Discussions.get(ownerID))) // owned by ownerID
            .flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          newTitle <- ParseNewtype[IO].parse[Post.Title]("unauthorized update")
          // when
          response <- PostAPIs.update.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(otherSessionID)),
            channelID,
            postID,
            UpdatePostRequest(
              newTitle = Updatable.Set(newTitle),
              newContent = Updatable.Keep
            )
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[PostError.NoPermission]))
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/{postID} for non-existent Post" in {

      "return NoPermission for a non-existent Post (ownership is resolved before existence, so a missing entity " +
        "surfaces as NoPermission rather than leaking its non-existence)" in {
          for {
            // given
            CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
            _ <- discussionsReads.channelReads.requireById(channelID).eventually()
            fakePostID = ID[Post](java.util.UUID.randomUUID())
            // when
            response <- PostAPIs.read.toTestCall.untupled(None, channelID, fakePostID)
          } yield {
            // then
            response.code === StatusCode.Unauthorized
            response.body must beValid(beLeft(beAnInstanceOf[PostError.NoPermission]))
          }
        }
    }

    "on DELETE /discussions/channels/{channelID}/posts/{postID} by non-owner" in {

      "return 401 when User is not the Post's Author and has no moderator permission" in {
        for {
          // given
          (CreationScheduled(ownerID), CreationScheduled(_)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(ownerID).eventually()
          (CreationScheduled(otherUserID), CreationScheduled(otherSessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(otherUserID).eventually()
          _ <- usersReads.sessionReads.requireById(otherSessionID).eventually()
          CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          CreationScheduled(postID) <- postCreate(channelID)
            .map(_.modify(_.authorID).setTo(userIDUsers2Discussions.get(ownerID))) // owned by ownerID
            .flatMap(discussionsWrites.postWrites.createPost)
          _ <- discussionsReads.postReads.requireById(postID).eventually()
          // when
          response <- PostAPIs.delete.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(otherSessionID)),
            channelID,
            postID
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[PostError.NoPermission]))
        }
      }
    }
  }
}
