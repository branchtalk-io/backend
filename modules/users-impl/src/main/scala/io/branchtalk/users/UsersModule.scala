package io.branchtalk.users

import cats.data.NonEmptyList
import cats.effect.{ Async, Resource }
import cats.effect.std.Dispatcher
import com.softwaremill.macwire.wire
import io.branchtalk.discussions.events.DiscussionEvent
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.model.{ Logger, UUID, UUIDGenerator }
import io.branchtalk.users.events.{ UsersCommandEvent, UsersEvent }
import io.branchtalk.users.reads._
import io.branchtalk.users.writes._
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
    domainConfig: DomainConfig,
    registry:     CollectorRegistry
  ): Resource[F, UsersReads[F]] =
    Logger.getLogger[F].pipe { logger =>
      Resource.make(logger.info("Initialize Users reads"))(_ => logger.info("Shut down Users reads"))
    } >>
      module.setupReads[F](domainConfig, registry).map { case ReadsInfrastructure(transactor, _) =>
        val userReads:    UserReads[F]    = wire[UserReadsImpl[F]]
        val sessionReads: SessionReads[F] = wire[SessionReadsImpl[F]]
        val banReads:     BanReads[F]     = wire[BanReadsImpl[F]]

        wire[UsersReads[F]]
      }

  // scalastyle:off method.length
  def writes[F[_]: Async: Dispatcher: MDC](
    domainConfig:           DomainConfig,
    registry:               CollectorRegistry
  )(implicit uuidGenerator: UUIDGenerator): Resource[F, UsersWrites[F]] =
    Logger.getLogger[F].pipe { logger =>
      Resource.make(logger.info("Initialize Users writes"))(_ => logger.info("Shut down Users writes")) >>
        module.setupWrites[F](domainConfig, registry).map {
          case WritesInfrastructure(transactor,
                                    internalProducer,
                                    internalConsumerStream,
                                    producer,
                                    consumerStream,
                                    cache
              ) =>
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
                ConsumerStream.noID
                  .andThen(discussionsConsumer)
                  .andThen(internalProducer)
                  .andThen(ConsumerStream.produced)
              )

            wire[UsersWrites[F]]
        }
    }
  // scalastyle:on method.length

  def listenToUsers[F[_]](domainConfig: DomainConfig)(
    discussionEventConsumer:            ConsumerStream.Factory[F, DiscussionEvent],
    runDiscussionsConsumer:             StreamRunner.FromConsumerStream[F, DiscussionEvent]
  ): StreamRunner[F] =
    (discussionEventConsumer andThen runDiscussionsConsumer)(domainConfig.consumers(discussionsProjectionName))
}
