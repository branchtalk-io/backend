package io.branchtalk.notifications

import cats.data.NonEmptyList
import cats.effect.{ Async, Resource }
import cats.effect.std.Dispatcher
import io.branchtalk.discussions.events.DiscussionEvent
import io.branchtalk.notifications.events.{ NotificationCommandEvent, NotificationEvent }
import io.branchtalk.notifications.reads.*
import io.branchtalk.notifications.writes.*
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.logging.MDC
import io.prometheus.client.CollectorRegistry

import scala.annotation.nowarn

final case class NotificationsReads[F[_]](
  notificationReads:         NotificationReads[F],
  notificationEventConsumer: ConsumerStream.Factory[F, NotificationEvent]
)

final case class NotificationsWrites[F[_]](
  notificationWrites:     NotificationWrites[F],
  runProjections:         StreamRunner[F],
  runDiscussionsConsumer: StreamRunner.FromConsumerStream[F, DiscussionEvent]
)

object NotificationsModule {

  private val module = DomainModule[NotificationEvent, NotificationCommandEvent]

  // same as in notifications.conf
  val postgresProjectionName    = "postgres-projection"
  val discussionsProjectionName = "discussions"

  def reads[F[_]: Async](
    domainConfig: DomainModule.Config,
    registry:     CollectorRegistry
  ): Resource[F, NotificationsReads[F]] =
    for {
      logger <- Resource.eval(Logger.create[F])
      _ <- Resource.make(logger.info("Initialize Notifications reads"))(_ =>
        logger.info("Shut down Notifications reads")
      )
      case Reads.Infrastructure(transactor, consumer) <- module.setupReads[F](domainConfig, logger, registry)
    } yield {
      val notificationReads: NotificationReads[F] = NotificationReadsImpl[F](transactor)

      NotificationsReads(notificationReads, consumer)
    }

  def writes[F[_]: Async: Dispatcher: MDC](
    domainConfig:            DomainModule.Config,
    discussionsDomainConfig: DomainModule.Config,
    registry:                CollectorRegistry,
    notificationTopic:       NotificationTopic[F]
  )(using UUID.Generator): Resource[F, NotificationsWrites[F]] =
    for {
      logger <- Resource.eval(Logger.create[F])
      _ <- Resource.make(logger.info("Initialize Notifications writes"))(_ =>
        logger.info("Shut down Notifications writes")
      )
      case Writes.Infrastructure(transactor,
                                 internalProducer,
                                 internalConsumerStream,
                                 producer,
                                 consumerStream,
                                 cache
      ) <- module.setupWrites[F](domainConfig, logger, registry)
      // We also need a transactor pointing at the discussions DB for the DiscussionsConsumer to look up
      // post/comment authors. Since notifications shares the same Postgres instance as discussions,
      // we use the discussions domain config for a second transactor.
      discussionsTransactor <- new PostgresDatabase(discussionsDomainConfig.databaseReads).transactor(logger, registry)
    } yield {
      val notificationWrites: NotificationWrites[F] =
        NotificationWritesImpl[F](internalProducer, transactor)

      val commandHandler: Projector[F, NotificationCommandEvent, (UUID, NotificationEvent)] = NonEmptyList
        .of(
          NotificationCommandHandler[F]
        )
        .reduce
      val postgresProjector: Projector[F, NotificationEvent, (UUID, NotificationEvent)] = NonEmptyList
        .of(
          NotificationPostgresProjector[F](transactor, notificationTopic)
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

      val discussionsConsumer: DiscussionsConsumer[F] = DiscussionsConsumer[F](discussionsTransactor)
      val runDiscussionsConsumer: StreamRunner.FromConsumerStream[F, DiscussionEvent] =
        _.runThrough(logger)(
          ConsumerStream.noID.andThen(discussionsConsumer).andThen(internalProducer).andThen(ConsumerStream.produced)
        )

      NotificationsWrites(notificationWrites, runProjections, runDiscussionsConsumer)
    }

  def listenToDiscussions[F[_]](domainConfig: DomainModule.Config)(
    discussionEventConsumer: ConsumerStream.Factory[F, DiscussionEvent],
    runDiscussionsConsumer:  StreamRunner.FromConsumerStream[F, DiscussionEvent]
  ): StreamRunner[F] =
    (discussionEventConsumer andThen runDiscussionsConsumer)(domainConfig.consumers(discussionsProjectionName))
}
