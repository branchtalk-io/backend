package io.branchtalk.users

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import com.softwaremill.macwire.wire
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.models.{ Logger, UUID, UUIDGenerator }
import io.branchtalk.users.events.{ UsersCommandEvent, UsersEvent }
import io.branchtalk.users.reads._
import io.branchtalk.users.writes._
import io.prometheus.client.CollectorRegistry

final case class UsersReads[F[_]](
  userReads:    UserReads[F],
  sessionReads: SessionReads[F]
)

final case class UsersWrites[F[_]](
  userWrites:    UserWrites[F],
  sessionWrites: SessionWrites[F],
  runProjector:  Resource[F, F[Unit]]
)

object UsersModule {

  private val module = DomainModule[UsersEvent, UsersCommandEvent]

  def reads[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig,
    registry:     CollectorRegistry
  ): Resource[F, UsersReads[F]] =
    Logger.getLogger[F].pipe { logger =>
      Resource.make(logger.info("Initialize Users reads"))(_ => logger.info("Shut down Users reads"))
    } >>
      module.setupReads[F](domainConfig, registry).map { case ReadsInfrastructure(transactor, _) =>
        val userReads:    UserReads[F]    = wire[UserReadsImpl[F]]
        val sessionReads: SessionReads[F] = wire[SessionReadsImpl[F]]

        wire[UsersReads[F]]
      }

  def writes[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig:           DomainConfig,
    registry:               CollectorRegistry
  )(implicit uuidGenerator: UUIDGenerator): Resource[F, UsersWrites[F]] =
    Logger.getLogger[F].pipe { logger =>
      Resource.make(logger.info("Initialize Users writes"))(_ => logger.info("Shut down Users writes")) >>
        module.setupWrites[F](domainConfig, registry).map {
          case WritesInfrastructure(transactor, internalProducer, internalConsumerStream, producer) =>
            val userWrites:    UserWrites[F]    = wire[UserWritesImpl[F]]
            val sessionWrites: SessionWrites[F] = wire[SessionWritesImpl[F]]

            val projector: Projector[F, UsersCommandEvent, (UUID, UsersEvent)] = NonEmptyList
              .of(
                wire[UserProjector[F]]: Projector[F, UsersCommandEvent, (UUID, UsersEvent)]
              )
              .reduce
            val runProjector: Resource[F, F[Unit]] =
              internalConsumerStream.withPipeToResource(logger)(projector andThen producer)

            wire[UsersWrites[F]]
        }
    }
}
