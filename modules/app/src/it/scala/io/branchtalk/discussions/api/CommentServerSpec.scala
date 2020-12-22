package io.branchtalk.discussions.api

import io.branchtalk.api.{ Authentication, Pagination, PaginationLimit, PaginationOffset, ServerIOTest }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.CommentModels._
import io.branchtalk.discussions.model.Comment
import io.branchtalk.mappings._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import io.scalaland.chimney.dsl._
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class CommentServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  implicit protected lazy val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "CommentServer-provided endpoints" should {

    "on GET /discussions/channels/{channelID}/posts/{postID}/comments/newest" in {

      "return newest Comments for a specified Post" in {
        withAllProjections {

          for {
            // given
            CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
            _ <- discussionsReads.channelReads.requireById(channelID).eventually()
            CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
            _ <- discussionsReads.postReads.requireById(postID).eventually()
            commentIDs <- (0 until 10).toList.traverse(_ =>
              commentCreate(postID).flatMap(discussionsWrites.commentWrites.createComment).map(_.id)
            )
            comments <- commentIDs.traverse(discussionsReads.commentReads.requireById(_)).eventually()
            // when
            response1 <- CommentAPIs.newest.toTestCall.untupled(None,
                                                                channelID,
                                                                postID,
                                                                None,
                                                                PaginationLimit(5).some,
                                                                None
            )
            response2 <- CommentAPIs.newest.toTestCall.untupled(None,
                                                                channelID,
                                                                postID,
                                                                PaginationOffset(5L).some,
                                                                PaginationLimit(5).some,
                                                                None
            )
          } yield {
            // then
            response1.code must_=== StatusCode.Ok
            response1.body must beValid(beRight(anInstanceOf[Pagination[APIComment]]))
            response2.code must_=== StatusCode.Ok
            response2.body must beValid(beRight(anInstanceOf[Pagination[APIComment]]))
            (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
              .mapN { (pagination1, pagination2) =>
                (pagination1.entities.toSet ++ pagination2.entities.toSet) must_=== comments
                  .map(APIComment.fromDomain)
                  .toSet
              }
              .getOrElse(pass)
          }
        }
      }
    }

    "on POST /discussions/channels/{channelID}/posts/{postID}/comments" in {

      "create a new Comment" in {
        withAllProjections {

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
            creationData <- commentCreate(postID)
            // when
            response <- CommentAPIs.create.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              channelID,
              postID,
              creationData.transformInto[CreateCommentRequest]
            )
            // TODO: check that this creates a new comment eventually!
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(anInstanceOf[CreateCommentResponse]))
          }
        }
      }
    }

    "on GET /discussions/channels/{channelID}/posts/{postID}/{postID}/comments/{commentID}" in {

      "fetch existing Post" in {
        withAllProjections {

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
            CreationScheduled(commentID) <- commentCreate(postID).flatMap(
              discussionsWrites.commentWrites.createComment
            )
            comment <- discussionsReads.commentReads.requireById(commentID).eventually()
            // when
            response <- CommentAPIs.read.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)).some,
              channelID,
              postID,
              commentID
            )
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(be_===(APIComment.fromDomain(comment))))
          }
        }
      }
    }

    "on PUT /discussions/channels/{channelID}/posts/{postID}/comments/{commentID}" in {

      "update existing Comment when User is its Author" in {
        withAllProjections {

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
            CreationScheduled(commentID) <- commentCreate(postID)
              .map(_.lens(_.authorID).set(userIDUsers2Discussions.get(userID))) // to own the Comment
              .flatMap(discussionsWrites.commentWrites.createComment)
            comment <- discussionsReads.commentReads.requireById(commentID).eventually()
            newContent = Comment.Content("lorem ipsum")
            // when
            response <- CommentAPIs.update.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              channelID,
              postID,
              commentID,
              UpdateCommentRequest(
                newContent = Updatable.Set(newContent)
              )
            )
            updatedComment <- discussionsReads.commentReads
              .requireById(commentID)
              .assert("Updated entity should have lastModifiedAt set")(_.data.lastModifiedAt.isDefined)
              .eventually()
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(be_===(UpdateCommentResponse(commentID))))
            updatedComment must_=== comment
              .lens(_.data.content)
              .set(newContent)
              .lens(_.data.lastModifiedAt)
              .set(updatedComment.data.lastModifiedAt)
          }
        }
      }
    }

    "on DELETE /discussions/channels/{channelID}/posts/{postID}/comments/{commentID}" in {

      "delete existing Comment when User is its Author" in {
        withAllProjections {

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
            CreationScheduled(commentID) <- commentCreate(postID)
              .map(_.lens(_.authorID).set(userIDUsers2Discussions.get(userID))) // to own the Comment
              .flatMap(discussionsWrites.commentWrites.createComment)
            _ <- discussionsReads.commentReads.requireById(commentID).eventually()
            // when
            response <- CommentAPIs.delete.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              channelID,
              postID,
              commentID
            )
            _ <- discussionsReads.commentReads
              .deleted(commentID)
              .assert("Comment should be eventually deleted")(identity)
              .eventually()
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(be_===(DeleteCommentResponse(commentID))))
          }
        }
      }
    }

    "on POST /discussions/channels/{channelID}/posts/{postID}/comments/{commentID}/restore" in {

      "restore deleted Comment when User is its Author" in {
        withAllProjections {

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
            CreationScheduled(commentID) <- commentCreate(postID)
              .map(_.lens(_.authorID).set(userIDUsers2Discussions.get(userID))) // to own the Comment
              .flatMap(discussionsWrites.commentWrites.createComment)
            _ <- discussionsReads.commentReads.requireById(commentID).eventually()
            _ <- discussionsWrites.commentWrites.deleteComment(
              Comment.Delete(commentID, userIDUsers2Discussions.get(userID))
            )
            _ <- discussionsReads.commentReads.requireById(commentID, isDeleted = true).eventually()
            // when
            response <- CommentAPIs.restore.toTestCall.untupled(
              Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
              channelID,
              postID,
              commentID
            )
            _ <- discussionsReads.postReads
              .exists(postID)
              .assert("Comment should be eventually restored")(identity)
              .eventually()
          } yield {
            // then
            response.code must_=== StatusCode.Ok
            response.body must beValid(beRight(be_===(RestoreCommentResponse(commentID))))
          }
        }
      }
    }
  }
}
