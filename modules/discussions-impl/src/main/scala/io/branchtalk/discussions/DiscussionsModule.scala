package io.branchtalk.discussions

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import io.branchtalk.discussions.events.{ DiscussionEvent, DiscussionsCommandEvent }
import io.branchtalk.discussions.reads._
import io.branchtalk.discussions.writes._
import io.branchtalk.shared.model._
import io.branchtalk.shared.infrastructure._
import com.softwaremill.macwire.wire
import io.branchtalk.logging.MDC
import io.prometheus.client.CollectorRegistry

import scala.annotation.nowarn

final case class DiscussionsReads[F[_]](
  channelReads:            ChannelReads[F],
  postReads:               PostReads[F],
  commentReads:            CommentReads[F],
  subscriptionReads:       SubscriptionReads[F],
  discussionEventConsumer: ConsumerStream.Builder[F, DiscussionEvent]
)

final case class DiscussionsWrites[F[_]](
  commentWrites:      CommentWrites[F],
  postWrites:         PostWrites[F],
  channelWrites:      ChannelWrites[F],
  subscriptionWrites: SubscriptionWrites[F],
  runProjector:       ConsumerStream.AsResource[F]
)

@nowarn("cat=unused") // macwire
object DiscussionsModule {

  private val module = DomainModule[DiscussionEvent, DiscussionsCommandEvent]

  // same as in discussions.conf
  val postgresProjectionName = "postgres-projection"

  def reads[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig,
    registry:     CollectorRegistry
  ): Resource[F, DiscussionsReads[F]] =
    Logger.getLogger[F].pipe { logger =>
      Resource.make(logger.info("Initialize Discussions reads"))(_ => logger.info("Shut down Discussions reads"))
    } >>
      module.setupReads[F](domainConfig, registry).map { case ReadsInfrastructure(transactor, consumer) =>
        val channelReads:      ChannelReads[F]      = wire[ChannelReadsImpl[F]]
        val postReads:         PostReads[F]         = wire[PostReadsImpl[F]]
        val commentReads:      CommentReads[F]      = wire[CommentReadsImpl[F]]
        val subscriptionReads: SubscriptionReads[F] = wire[SubscriptionReadsImpl[F]]

        wire[DiscussionsReads[F]]
      }

  def writes[F[_]: ConcurrentEffect: ContextShift: Timer: MDC](
    domainConfig:           DomainConfig,
    registry:               CollectorRegistry
  )(implicit uuidGenerator: UUIDGenerator): Resource[F, DiscussionsWrites[F]] =
    Logger.getLogger[F].pipe { logger =>
      Resource.make(logger.info("Initialize Discussions writes"))(_ => logger.info("Shut down Discussions writes")) >>
        module.setupWrites[F](domainConfig, registry).map {
          case WritesInfrastructure(transactor,
                                    internalProducer,
                                    internalConsumerStream,
                                    producer,
                                    consumerStream,
                                    cache
              ) =>
            val channelWrites:      ChannelWrites[F]      = wire[ChannelWritesImpl[F]]
            val postWrites:         PostWrites[F]         = wire[PostWritesImpl[F]]
            val commentWrites:      CommentWrites[F]      = wire[CommentWritesImpl[F]]
            val subscriptionWrites: SubscriptionWrites[F] = wire[SubscriptionWritesImpl[F]]

            val commandHandler: Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] = NonEmptyList
              .of(
                wire[ChannelCommandHandler[F]],
                wire[PostCommandHandler[F]],
                wire[CommentCommandHandler[F]],
                wire[SubscriptionCommandHandler[F]]
              )
              .reduce
            val postgresProjector: Projector[F, DiscussionEvent, (UUID, DiscussionEvent)] = NonEmptyList
              .of(
                wire[ChannelPostgresProjector[F]],
                wire[CommentPostgresProjector[F]],
                wire[PostPostgresProjector[F]],
                wire[SubscriptionPostgresProjector[F]]
              )
              .reduce
            val runProjector: ConsumerStream.AsResource[F] = {
              val runCommandProjector: ConsumerStream.AsResource[F] =
                internalConsumerStream.withCachedPipeToResource(logger, cache)(
                  ConsumerStream.noID.andThen(commandHandler).andThen(producer).andThen(ConsumerStream.produced)
                )
              val runPostgresProjector: ConsumerStream.AsResource[F] =
                consumerStream(domainConfig.consumers(postgresProjectionName)).withCachedPipeToResource(logger, cache)(
                  ConsumerStream.noID.andThen(postgresProjector).andThen(ConsumerStream.noID)
                )
              ConsumerStream.mergeResources(runCommandProjector, runPostgresProjector)
            }

            wire[DiscussionsWrites[F]]
        }
    }
}
