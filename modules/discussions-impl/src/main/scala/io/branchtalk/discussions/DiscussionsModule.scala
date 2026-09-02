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
import io.branchtalk.logging.MDC
import io.prometheus.client.CollectorRegistry
import org.ekrich.config.ConfigFactory

import scala.annotation.nowarn
import scala.util.Try

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

object DiscussionsModule {

  private val module = DomainModule[DiscussionEvent, DiscussionsCommandEvent]

  // same as in discussions.conf
  val postgresProjectionName = "postgres-projection"

  def reads[F[_]: Async](
    domainConfig: DomainModule.Config,
    registry:     CollectorRegistry
  ): Resource[F, DiscussionsReads[F]] =
    for {
      logger <- Resource.eval(Logger.create[F])
      _ <- Resource.make(logger.info("Initialize Discussions reads"))(_ => logger.info("Shut down Discussions reads"))
      case Reads.Infrastructure(transactor, consumer) <- module.setupReads[F](domainConfig, logger, registry)
    } yield {
      // macwire got removed due to:
      // https://github.com/softwaremill/macwire/blob/abf95284e24138a984a06ef4f08d0788af371825/macros/src/main/scala-3/com/softwaremill/macwire/internals/ConstructorCrimper.scala#L91
      val channelReads:      ChannelReads[F]      = ChannelReadsImpl[F](transactor)
      val postReads:         PostReads[F]         = PostReadsImpl[F](transactor)
      val commentReads:      CommentReads[F]      = CommentReadsImpl[F](transactor)
      val subscriptionReads: SubscriptionReads[F] = SubscriptionReadsImpl[F](transactor)

      DiscussionsReads(channelReads, postReads, commentReads, subscriptionReads, consumer)
    }

  def writes[F[_]: Async: Dispatcher: MDC](
    domainConfig: DomainModule.Config,
    registry:     CollectorRegistry
  )(using UUID.Generator): Resource[F, DiscussionsWrites[F]] =
    for {
      logger <- Resource.eval(Logger.create[F])
      _ <- Resource.make(logger.info("Initialize Discussions writes"))(_ => logger.info("Shut down Discussions writes"))
      case Writes.Infrastructure(transactor,
                                 internalProducer,
                                 internalConsumerStream,
                                 producer,
                                 consumerStream,
                                 cache
      ) <- module.setupWrites[F](domainConfig, logger, registry)
    } yield {
      // macwire got removed due to:
      // https://github.com/softwaremill/macwire/blob/abf95284e24138a984a06ef4f08d0788af371825/macros/src/main/scala-3/com/softwaremill/macwire/internals/ConstructorCrimper.scala#L91
      val urlTitleMaxLength: Int = Try {
        ConfigFactory.defaultApplication().resolve().getInt("discussions.url-title-max-length")
      }.getOrElse(100)

      val channelWrites:      ChannelWrites[F]      = ChannelWritesImpl[F](internalProducer, transactor)
      val postWrites:         PostWrites[F]         = PostWritesImpl[F](internalProducer, transactor, urlTitleMaxLength)
      val commentWrites:      CommentWrites[F]      = CommentWritesImpl[F](internalProducer, transactor)
      val subscriptionWrites: SubscriptionWrites[F] = SubscriptionWritesImpl[F](internalProducer, transactor)

      val commandHandler: Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] = NonEmptyList
        .of(
          ChannelCommandHandler[F],
          PostCommandHandler[F],
          CommentCommandHandler[F],
          SubscriptionCommandHandler[F]
        )
        .reduce
      val postgresProjector: Projector[F, DiscussionEvent, (UUID, DiscussionEvent)] = NonEmptyList
        .of(
          ChannelPostgresProjector[F](transactor),
          CommentPostgresProjector[F](transactor),
          PostPostgresProjector[F](transactor),
          SubscriptionPostgresProjector[F](transactor)
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

      DiscussionsWrites(commentWrites, postWrites, channelWrites, subscriptionWrites, runProjector)
    }
}
