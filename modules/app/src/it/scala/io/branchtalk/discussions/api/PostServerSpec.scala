package io.branchtalk.discussions.api

import cats.effect.IO
import io.branchtalk.api.{ Authentication, Pagination, PaginationLimit, PaginationOffset, ServerIOTest }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.{ Channel, Post, Subscription }
import io.branchtalk.mappings._
import io.branchtalk.shared.models._
import io.branchtalk.users.UsersFixtures
import io.scalaland.chimney.dsl._
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class PostServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  private lazy val defaultChannelID = ID[Channel](UUIDGenerator.FastUUIDGenerator.create[IO].unsafeRunSync())
  implicit protected lazy val uuidGenerator: TestUUIDGenerator =
    (new TestUUIDGenerator).tap(_.stubNext(defaultChannelID.uuid)) // stub generation in ServerIOTest resources

  "PostServer-provided endpoints" should {

    "on GET /discussions/posts/newest" in {

      "return newest Posts for default Channels for signed-out User" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              _ = uuidGenerator.stubNext(defaultChannelID.uuid) // makes sure that created Channel will have our ID
              CreationScheduled(channelID) <- channelCreate
                .flatMap(discussionsWrites.channelWrites.createChannel)
                .assert("Created Channel should have predefined ID")(_.id === defaultChannelID)
              _ <- discussionsReads.channelReads.requireById(channelID).eventually()
              postIDs <- (0 until 10).toList.traverse(_ =>
                postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
              )
              posts <- postIDs.traverse(discussionsReads.postReads.requireById).eventually()
              // when
              response1 <- PostAPIs.newest.toTestCall.untupled(None, None, PaginationLimit(5).some)
              response2 <- PostAPIs.newest.toTestCall.untupled(None, PaginationOffset(5L).some, PaginationLimit(5).some)
            } yield {
              // then
              response1.code must_=== StatusCode.Ok
              response1.body must beValid(beRight(anInstanceOf[Pagination[APIPost]]))
              response2.code must_=== StatusCode.Ok
              response2.body must beValid(beRight(anInstanceOf[Pagination[APIPost]]))
              (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
                .mapN { (pagination1, pagination2) =>
                  (pagination1.entities.toSet ++ pagination2.entities.toSet) must_=== posts
                    .map(APIPost.fromDomain)
                    .toSet
                }
                .getOrElse(true must beTrue)
            }
        }
      }

      "return newest Posts for Channels subscribed by current User" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              _ <- discussionsReads.channelReads.requireById(channelID).eventually()
              subscriberID = userIDUsers2Discussions.get(userID)
              _ <- discussionsWrites.subscriptionWrites.subscribe(
                Subscription.Subscribe(subscriberID = subscriberID, subscriptions = Set(channelID))
              )
              _ <- discussionsReads.subscriptionReads
                .requireForUser(subscriberID)
                .assert("Subscriptions should contain added Channel ID")(_.subscriptions(channelID))
                .eventually()
              postIDs <- (0 until 10).toList.traverse(_ =>
                postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.id)
              )
              posts <- postIDs.traverse(discussionsReads.postReads.requireById).eventually()
              // when
              response1 <- PostAPIs.newest.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)).some,
                None,
                PaginationLimit(5).some
              )
              response2 <- PostAPIs.newest.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)).some,
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
                  (pagination1.entities.toSet ++ pagination2.entities.toSet) must_=== posts
                    .map(APIPost.fromDomain)
                    .toSet
                }
                .getOrElse(true must beTrue)
            }
        }
      }
    }

    "on POST /discussions/posts" in {

      "create a new Post" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              _ <- discussionsReads.channelReads.requireById(channelID).eventually()
              subscriberID = userIDUsers2Discussions.get(userID)
              _ <- discussionsWrites.subscriptionWrites.subscribe(
                Subscription.Subscribe(subscriberID = subscriberID, subscriptions = Set(channelID))
              )
              _ <- discussionsReads.subscriptionReads
                .requireForUser(subscriberID)
                .assert("Subscriptions should contain added Channel ID")(_.subscriptions(channelID))
                .eventually()
              creationData <- postCreate(channelID)
              // when
              response <- PostAPIs.create.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
                creationData.transformInto[CreatePostRequest]
              )
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(anInstanceOf[CreatePostResponse]))
            }
        }
      }
    }

    "on GET /discussions/posts/{postID}" in {

      "fetch existing Post" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              _ <- discussionsReads.channelReads.requireById(channelID).eventually()
              subscriberID = userIDUsers2Discussions.get(userID)
              _ <- discussionsWrites.subscriptionWrites.subscribe(
                Subscription.Subscribe(subscriberID = subscriberID, subscriptions = Set(channelID))
              )
              _ <- discussionsReads.subscriptionReads
                .requireForUser(subscriberID)
                .assert("Subscriptions should contain added Channel ID")(_.subscriptions(channelID))
                .eventually()
              CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
              post <- discussionsReads.postReads.requireById(postID).eventually()
              // when
              response <- PostAPIs.read.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)).some,
                postID
              )
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(be_===(APIPost.fromDomain(post))))
            }
        }
      }
    }

    "on PUT /discussions/posts/{postID}" in {

      "update existing Post" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              _ <- discussionsReads.channelReads.requireById(channelID).eventually()
              subscriberID = userIDUsers2Discussions.get(userID)
              _ <- discussionsWrites.subscriptionWrites.subscribe(
                Subscription.Subscribe(subscriberID = subscriberID, subscriptions = Set(channelID))
              )
              _ <- discussionsReads.subscriptionReads
                .requireForUser(subscriberID)
                .assert("Subscriptions should contain added Channel ID")(_.subscriptions(channelID))
                .eventually()
              CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
              post <- discussionsReads.postReads.requireById(postID).eventually()
              newTitle <- Post.Title.parse[IO]("new title")
              newContent = Post.Content.Text(Post.Text("lorem ipsum"))
              // when
              response <- PostAPIs.update.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
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
                .lens(_.data.title)
                .set(newTitle)
                .lens(_.data.content)
                .set(newContent)
                .lens(_.data.urlTitle)
                .set(Post.UrlTitle("url-title"))
                .lens(_.data.lastModifiedAt)
                .set(updatedPost.data.lastModifiedAt)
            }
        }
      }
    }

    "on DELETE /discussions/posts/{postID}" in {

      "update existing Post" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              _ <- discussionsReads.channelReads.requireById(channelID).eventually()
              subscriberID = userIDUsers2Discussions.get(userID)
              _ <- discussionsWrites.subscriptionWrites.subscribe(
                Subscription.Subscribe(subscriberID = subscriberID, subscriptions = Set(channelID))
              )
              _ <- discussionsReads.subscriptionReads
                .requireForUser(subscriberID)
                .assert("Subscriptions should contain added Channel ID")(_.subscriptions(channelID))
                .eventually()
              CreationScheduled(postID) <- postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost)
              _ <- discussionsReads.postReads.requireById(postID).eventually()
              // when
              response <- PostAPIs.delete.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
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
    }

    // TODO: restore
  }
}
