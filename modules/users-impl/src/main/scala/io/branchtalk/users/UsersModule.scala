package io.branchtalk.users

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import com.softwaremill.macwire.wire
import com.typesafe.scalalogging.Logger
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.models.{ UUID, UUIDGenerator }
import io.branchtalk.users.events.{ UsersCommandEvent, UsersEvent }
import io.branchtalk.users.reads._
import io.branchtalk.users.writes._

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
  private val logger = Logger(getClass)

  def reads[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, UsersReads[F]] = module.setupReads[F](domainConfig).map { case ReadsInfrastructure(transactor, _) =>
    val userReads:    UserReads[F]    = wire[UserReadsImpl[F]]
    val sessionReads: SessionReads[F] = wire[SessionReadsImpl[F]]

    wire[UsersReads[F]]
  }

  def writes[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig:           DomainConfig
  )(implicit uuidGenerator: UUIDGenerator): Resource[F, UsersWrites[F]] = module.setupWrites[F](domainConfig).map {
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
