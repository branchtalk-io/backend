package io.branchtalk.discussions

import io.branchtalk.discussions.model.Subscription
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import org.specs2.mutable.Specification

final class SubscriptionReadsWritesSpec extends Specification, DiscussionsIOTest, DiscussionsFixtures {

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Subscription Reads & Writes" should {

    "add Subscription and eventually read it" in {
      for {
        // given
        subscriberID <- subscriberIDCreate
        ids <- (0 until 3).toList.traverse { _ =>
          channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.unwrap)
        }
        _ <- ids.traverse(discussionsReads.channelReads.requireById(_)).eventually()
        // when
        _ <- discussionsWrites.subscriptionWrites.subscribe(Subscription.Subscribe(subscriberID, ids.toSet))
        subscription <- discussionsReads.subscriptionReads
          .requireForUser(subscriberID)
          .assert("Subscriptions should be eventually added")(_.subscriptions eqv ids.toSet)
          .eventually()
      } yield
      // then
      subscription === Subscription(subscriberID, ids.toSet)
    }

    "remove Subscription and eventually read it" in {
      for {
        // given
        subscriberID <- subscriberIDCreate
        idsToKeep <- (0 until 3).toList.traverse { _ =>
          channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.unwrap)
        }
        idsToRemove <- (0 until 3).toList.traverse { _ =>
          channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.unwrap)
        }
        ids = idsToKeep ++ idsToRemove
        _ <- ids.traverse(discussionsReads.channelReads.requireById(_)).eventually()
        _ <- discussionsWrites.subscriptionWrites.subscribe(Subscription.Subscribe(subscriberID, ids.toSet))
        _ <- discussionsReads.subscriptionReads
          .requireForUser(subscriberID)
          .assert("Subscriptions should be eventually added")(_.subscriptions eqv ids.toSet)
          .eventually()
        // when
        Subscription.Scheduled(left) <- discussionsWrites.subscriptionWrites.unsubscribe(
          Subscription.Unsubscribe(subscriberID, idsToRemove.toSet)
        )
        subscription <- discussionsReads.subscriptionReads
          .requireForUser(subscriberID)
          .assert("Subscriptions should be eventually deleted")(_.subscriptions eqv idsToKeep.toSet)
          .eventually()
      } yield {
        // then
        left === Subscription(subscriberID, idsToKeep.toSet)
        subscription === Subscription(subscriberID, idsToKeep.toSet)
      }
    }
  }
}
