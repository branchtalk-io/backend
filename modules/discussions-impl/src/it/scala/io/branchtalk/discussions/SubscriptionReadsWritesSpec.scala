package io.branchtalk.discussions

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.discussions.model.Subscription
import io.branchtalk.shared.models.UUIDGenerator
import org.specs2.mutable.Specification

final class SubscriptionReadsWritesSpec
    extends Specification
    with IOTest
    with ResourcefulTest
    with DiscussionsFixtures {

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  // populated by resources
  private var discussionsReads:  DiscussionsReads[IO]  = _
  private var discussionsWrites: DiscussionsWrites[IO] = _

  override protected def testResource: Resource[IO, Unit] =
    for {
      domainCfg <- TestDiscussionsConfig.loadDomainConfig[IO]
      reads <- DiscussionsModule.reads[IO](domainCfg)
      writes <- DiscussionsModule.writes[IO](domainCfg)
    } yield {
      discussionsReads  = reads
      discussionsWrites = writes
    }

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
          subscription === Subscription(subscriberID, ids.toSet)
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
          subscription === Subscription(subscriberID, idsToKeep.toSet)
        }
      }
    }
  }
}
