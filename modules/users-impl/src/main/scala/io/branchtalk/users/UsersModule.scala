package io.branchtalk.users

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
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
  ): Resource[F, UsersReads[F]] = module.setupReads[F](domainConfig).map {
    case ReadsInfrastructure(transactor, _) =>
      val userReads    = new UserReadsImpl[F](transactor)
      val sessionReads = new SessionReadsImpl[F](transactor)

      UsersReads(userReads, sessionReads)
  }

  // TODO: writes should test if they can write before they send event to bus
  def writes[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig:           DomainConfig
  )(implicit uuidGenerator: UUIDGenerator): Resource[F, UsersWrites[F]] = module.setupWrites[F](domainConfig).map {
    case WritesInfrastructure(transactor, internalProducer, internalConsumerStream, producer) =>
      val userWrites    = new UserWritesImpl[F](internalProducer, transactor)
      val sessionWrites = new SessionWritesImpl[F](producer, transactor)

      val projector: Projector[F, UsersCommandEvent, (UUID, UsersEvent)] = NonEmptyList
        .of(
          new UserProjector[F](transactor): Projector[F, UsersCommandEvent, (UUID, UsersEvent)]
        )
        .reduce
      val runProjector = internalConsumerStream.withPipeToResource(logger)(projector andThen producer)

      UsersWrites(userWrites, sessionWrites, runProjector)
  }
}
