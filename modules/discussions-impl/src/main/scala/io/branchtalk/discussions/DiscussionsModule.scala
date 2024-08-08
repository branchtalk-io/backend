package io.branchtalk.discussions

import cats.data.NonEmptyList
import cats.effect.{ Async, Resource }
import cats.effect.std.Dispatcher
import io.branchtalk.discussions.events.{ DiscussionEvent, DiscussionsCommandEvent }
import io.branchtalk.discussions.reads.*
import io.branchtalk.discussions.writes.*
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import com.softwaremill.macwire.wire
import io.branchtalk.logging.MDC
import io.prometheus.client.CollectorRegistry

import scala.annotation.nowarn

final case class DiscussionsReads[F[_]](
  channelReads:            ChannelReads[F],
  postReads:               PostReads[F],
  commentReads:            CommentReads[F],
  subscriptionReads:       SubscriptionReads[F],
  discussionEventConsumer: ConsumerStream.Factory[F, DiscussionEvent]
)

final case class DiscussionsWrites[F[_]](
  commentWrites:      CommentWrites[F],
  postWrites:         PostWrites[F],
  channelWrites:      ChannelWrites[F],
  subscriptionWrites: SubscriptionWrites[F],
  runProjecions:      StreamRunner[F]
)

@nowarn("cat=unused") // macwire
object DiscussionsModule {

  private val module = DomainModule[DiscussionEvent, DiscussionsCommandEvent]

  // same as in discussions.conf
  val postgresProjectionName = "postgres-projection"

  def reads[F[_]: Async](
    domainConfig: DomainModule.Config,
    registry:     CollectorRegistry
  ): Resource[F, DiscussionsReads[F]] =
    for {
      logger <- Resource.eval(Logger.getLogger[F])
      _ <- Resource.make(logger.info("Initialize Discussions reads"))(_ => logger.info("Shut down Discussions reads"))
      case Reads.Infrastructure(transactor, consumer) <- module.setupReads[F](domainConfig, registry)
    } yield {
      val channelReads:      ChannelReads[F]      = wire[ChannelReadsImpl[F]]
      val postReads:         PostReads[F]         = wire[PostReadsImpl[F]]
      val commentReads:      CommentReads[F]      = wire[CommentReadsImpl[F]]
      val subscriptionReads: SubscriptionReads[F] = wire[SubscriptionReadsImpl[F]]

      wire[DiscussionsReads[F]]
    }

  def writes[F[_]: Async: Dispatcher: MDC](
    domainConfig: DomainModule.Config,
    registry:     CollectorRegistry
  )(using UUID.Generator): Resource[F, DiscussionsWrites[F]] =
    for {
      logger <- Resource.eval(Logger.getLogger[F])
      _ <- Resource.make(logger.info("Initialize Discussions writes"))(_ => logger.info("Shut down Discussions writes"))
      case Writes.Infrastructure(transactor,
                                 internalProducer,
                                 internalConsumerStream,
                                 producer,
                                 consumerStream,
                                 cache
      ) <- module.setupWrites[F](domainConfig, registry)
    } yield {
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
      val runProjector: StreamRunner[F] = {
        val runCommandProjector: StreamRunner[F] =
          internalConsumerStream.runCachedThrough(logger, cache)(
            ConsumerStream.noID.andThen(commandHandler).andThen(producer).andThen(ConsumerStream.produced)
          )
        val runPostgresProjector: StreamRunner[F] =
          consumerStream(domainConfig.consumers(postgresProjectionName)).runCachedThrough(logger, cache)(
            ConsumerStream.noID.andThen(postgresProjector).andThen(ConsumerStream.noID)
          )
        runCommandProjector |+| runPostgresProjector
      }

      wire[DiscussionsWrites[F]]
    }
}
