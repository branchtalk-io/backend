package io.branchtalk.users

import cats.data.NonEmptyList
import cats.effect.{ Async, Resource }
import cats.effect.std.Dispatcher
import com.softwaremill.macwire.wire
import io.branchtalk.discussions.events.DiscussionEvent
import io.branchtalk.logging.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.shared.model.{ Logger, UUID }
import io.branchtalk.users.events.{ UsersCommandEvent, UsersEvent }
import io.branchtalk.users.reads.*
import io.branchtalk.users.writes.*
import io.prometheus.client.CollectorRegistry

import scala.annotation.nowarn

final case class UsersReads[F[_]](
  userReads:    UserReads[F],
  sessionReads: SessionReads[F],
  banReads:     BanReads[F]
)

final case class UsersWrites[F[_]](
  userWrites:             UserWrites[F],
  sessionWrites:          SessionWrites[F],
  banWrites:              BanWrites[F],
  runProjections:         StreamRunner[F],
  runDiscussionsConsumer: StreamRunner.FromConsumerStream[F, DiscussionEvent]
)
@nowarn("cat=unused") // macwire
object UsersModule {

  private val module = DomainModule[UsersEvent, UsersCommandEvent]

  // same as in users.conf
  val postgresProjectionName    = "postgres-projection"
  val discussionsProjectionName = "discussions"

  def reads[F[_]: Async](
    domainConfig: DomainModule.Config,
    registry:     CollectorRegistry
  ): Resource[F, UsersReads[F]] =
    for {
      logger <- Resource.eval(Logger.getLogger[F])
      _ <- Resource.make(logger.info("Initialize Users reads"))(_ => logger.info("Shut down Users reads"))
      case Reads.Infrastructure(transactor, _) <- module.setupReads[F](domainConfig, registry)
    } yield {
      val userReads:    UserReads[F]    = wire[UserReadsImpl[F]]
      val sessionReads: SessionReads[F] = wire[SessionReadsImpl[F]]
      val banReads:     BanReads[F]     = wire[BanReadsImpl[F]]

      wire[UsersReads[F]]
    }

  def writes[F[_]: Async: Dispatcher: MDC](
    domainConfig: DomainModule.Config,
    registry:     CollectorRegistry
  )(using UUID.Generator): Resource[F, UsersWrites[F]] =
    for {
      logger <- Resource.eval(Logger.getLogger[F])
      _ <- Resource.make(logger.info("Initialize Users writes"))(_ => logger.info("Shut down Users writes"))
      case Writes.Infrastructure(transactor,
                                 internalProducer,
                                 internalConsumerStream,
                                 producer,
                                 consumerStream,
                                 cache
      ) <- module.setupWrites[F](domainConfig, registry)
    } yield {
      val userWrites:    UserWrites[F]    = wire[UserWritesImpl[F]]
      val sessionWrites: SessionWrites[F] = wire[SessionWritesImpl[F]]
      val banWrites:     BanWrites[F]     = wire[BanWritesImpl[F]]

      val commandHandler: Projector[F, UsersCommandEvent, (UUID, UsersEvent)] = NonEmptyList
        .of(
          wire[UserCommandHandler[F]],
          wire[BanCommandHandler[F]]
        )
        .reduce
      val postgresProjector: Projector[F, UsersEvent, (UUID, UsersEvent)] = NonEmptyList
        .of(
          wire[UserPostgresProjector[F]],
          wire[BanPostgresProjector[F]]
        )
        .reduce
      val runProjections: StreamRunner[F] = {
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

      val discussionsConsumer: DiscussionsConsumer[F] = wire[DiscussionsConsumer[F]]
      val runDiscussionsConsumer: StreamRunner.FromConsumerStream[F, DiscussionEvent] =
        _.runThrough(logger)(
          ConsumerStream.noID.andThen(discussionsConsumer).andThen(internalProducer).andThen(ConsumerStream.produced)
        )

      wire[UsersWrites[F]]
    }

  def listenToUsers[F[_]](domainConfig: DomainModule.Config)(
    discussionEventConsumer: ConsumerStream.Factory[F, DiscussionEvent],
    runDiscussionsConsumer:  StreamRunner.FromConsumerStream[F, DiscussionEvent]
  ): StreamRunner[F] =
    (discussionEventConsumer andThen runDiscussionsConsumer)(domainConfig.consumers(discussionsProjectionName))
}
