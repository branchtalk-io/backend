package io.branchtalk.discussions.api

import cats.effect.IO
import io.branchtalk.api.{ Authentication, Pagination, ServerIOTest }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.PostModels.*
import io.branchtalk.discussions.model.{ Channel, Subscription }
import io.branchtalk.mappings.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.users.UsersFixtures
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class SubscriptionServerPaginationSpec extends Specification, ServerIOTest, UsersFixtures, DiscussionsFixtures {

  private val defaultChannelID = ID[Channel](java.util.UUID.randomUUID())
  protected given uuidGenerator: TestUUIDGenerator =
    (new TestUUIDGenerator).tap(_.stubNext(defaultChannelID.unwrap)) // stub generation in ServerIOTest resources

  "SubscriptionServer-provided pagination endpoints" should {

    "on GET /discussions/subscriptions/newest" in {

      "return paginated newest Posts for default Channels for signed-out User" in {
        for {
          // given
          CreationScheduled(channelID) <- channelCreate
            .flatTap(_ => IO(uuidGenerator.stubNext(defaultChannelID.unwrap))) // create Channel with default ID
            .flatMap(discussionsWrites.channelWrites.createChannel) // NOTE: ID generation must come before CID
            .assert("Created Channel should have predefined ID")(_.unwrap eqv defaultChannelID)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          postIDs <- (0 until 10).toList.traverse(_ =>
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.unwrap)
          )
          posts <- postIDs.traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          response1 <- SubscriptionAPIs.newest.toTestCall.untupled(None, None, Pagination.Limit(5).some)
          response2 <- SubscriptionAPIs.newest.toTestCall.untupled(None,
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

      "return paginated newest Posts for Channels subscribed by current User" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
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
            postCreate(channelID).flatMap(discussionsWrites.postWrites.createPost).map(_.unwrap)
          )
          posts <- postIDs.traverse(discussionsReads.postReads.requireById(_)).eventually()
          // when
          response1 <- SubscriptionAPIs.newest.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)).some,
            None,
            Pagination.Limit(5).some
          )
          response2 <- SubscriptionAPIs.newest.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)).some,
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
  }
}
