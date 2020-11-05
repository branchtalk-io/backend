package io.branchtalk.discussions

import io.branchtalk.discussions.model.Subscription
import io.branchtalk.shared.models.TestUUIDGenerator
import org.specs2.mutable.Specification

final class SubscriptionReadsWritesSpec extends Specification with DiscussionsIOTest with DiscussionsFixtures {

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Subscription Reads & Writes" should {

    "add Subscription and eventually read it" in {
      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          subscriberID <- subscriberIDCreate
          ids <- (0 until 3).toList.traverse { _ =>
            channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          }
          _ <- ids.traverse(discussionsReads.channelReads.requireById).eventually()
          // when
          _ <- discussionsWrites.subscriptionWrites.subscribe(Subscription.Subscribe(subscriberID, ids.toSet))
          subscription <- discussionsReads.subscriptionReads
            .requireForUser(subscriberID)
            .assert("Subscriptions should be eventually added")(_.subscriptions === ids.toSet)
            .eventually()
        } yield
        // then
        subscription must_=== Subscription(subscriberID, ids.toSet)
      }
    }

    "remove Subscription and eventually read it" in {

      discussionsWrites.runProjector.use { discussionsProjector =>
        for {
          // given
          _ <- discussionsProjector.logError("Error reported by Discussions projector").start
          subscriberID <- subscriberIDCreate
          idsToKeep <- (0 until 3).toList.traverse { _ =>
            channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          }
          idsToRemove <- (0 until 3).toList.traverse { _ =>
            channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          }
          ids = (idsToKeep ++ idsToRemove)
          _ <- ids.traverse(discussionsReads.channelReads.requireById).eventually()
          _ <- discussionsWrites.subscriptionWrites.subscribe(Subscription.Subscribe(subscriberID, ids.toSet))
          _ <- discussionsReads.subscriptionReads
            .requireForUser(subscriberID)
            .assert("Subscriptions should be eventually added")(_.subscriptions === ids.toSet)
            .eventually()
          // when
          _ <- discussionsWrites.subscriptionWrites.unsubscribe(
            Subscription.Unsubscribe(subscriberID, idsToRemove.toSet)
          )
          subscription <- discussionsReads.subscriptionReads
            .requireForUser(subscriberID)
            .assert("Subscriptions should be eventually deleted")(_.subscriptions === idsToKeep.toSet)
            .eventually()
        } yield
        // then
        subscription must_=== Subscription(subscriberID, idsToKeep.toSet)
      }
    }
  }
}
