package io.branchtalk.discussions

import cats.effect.IO
import io.branchtalk.discussions.model.Subscription
import io.branchtalk.shared.models.UUIDGenerator
import org.specs2.mutable.Specification

final class SubscriptionReadsWritesSpec extends Specification with DiscussionsIOTest with DiscussionsFixtures {

  protected implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  "Subscription Reads & Writes" should {

    "add Subscription and eventually read it" in {
      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.logError("Error reported by projector").start
          subscriberID <- subscriberIDCreate
          ids <- (0 until 3).toList.traverse { _ =>
            channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          }
          _ <- ids.traverse(discussionsReads.channelReads.requireById).eventually()
          // when
          _ <- discussionsWrites.subscriptionWrites.subscribe(Subscription.Subscribe(subscriberID, ids.toSet))
          subscription <- discussionsReads.subscriptionReads
            .requireForUser(subscriberID)
            .flatTap { subscription =>
              IO(assert(subscription.subscriptions === ids.toSet, "Subscriptions should be eventually added"))
            }
            .eventually()
        } yield {
          // then
          subscription must_=== Subscription(subscriberID, ids.toSet)
        }
      }
    }

    "remove Subscription and eventually read it" in {

      discussionsWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.logError("Error reported by projector").start
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
            .flatTap { subscription =>
              IO(assert(subscription.subscriptions === ids.toSet, "Subscriptions should be eventually added"))
            }
            .eventually()
          // when
          _ <- discussionsWrites.subscriptionWrites.unsubscribe(
            Subscription.Unsubscribe(subscriberID, idsToRemove.toSet)
          )
          subscription <- discussionsReads.subscriptionReads
            .requireForUser(subscriberID)
            .flatTap { subscription =>
              IO(assert(subscription.subscriptions === idsToKeep.toSet, "Subscriptions should be eventually deleted"))
            }
            .eventually()
        } yield {
          // then
          subscription must_=== Subscription(subscriberID, idsToKeep.toSet)
        }
      }
    }
  }
}
