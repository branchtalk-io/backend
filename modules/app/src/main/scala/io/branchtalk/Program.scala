package io.branchtalk

import cats.Applicative
import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, ExitCode, Resource, Sync, Timer }
import cats.effect.implicits._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.AppServer
import io.branchtalk.configs.{ APIConfig, AppConfig, Configuration }
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites }
import io.branchtalk.shared.infrastructure.DomainConfig
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.users.{ UsersModule, UsersReads, UsersWrites }
import io.prometheus.client.CollectorRegistry
import org.http4s.metrics.prometheus.Prometheus
import sun.misc.Signal

object Program {

  private val logger = Logger(getClass)

  implicit protected val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  def runApplication[F[_]: ConcurrentEffect: ContextShift: Timer](args: List[String]): F[ExitCode] =
    (for {
      env <- Configuration.getEnv[F]
      appConfig <- AppConfig.parse[F](args, env)
      apiConfig <- Configuration.readConfig[F, APIConfig]("api")
      usersConfig <- Configuration.readConfig[F, DomainConfig]("users")
      discussionsConfig <- Configuration.readConfig[F, DomainConfig]("discussions")
      _ <- Prometheus
        .collectorRegistry[F]
        .flatMap { registry => // TODO: pass registry to reads and writes to monitor Doobie queries
          (
            registry.pure[Resource[F, *]],
            UsersModule.reads[F](usersConfig, registry),
            UsersModule.writes[F](discussionsConfig, registry),
            DiscussionsModule.reads[F](discussionsConfig, registry),
            DiscussionsModule.writes[F](discussionsConfig, registry)
          ).tupled
        }
        .use((runModules[F](appConfig, apiConfig, awaitTerminationSignal[F]) _).tupled)
    } yield ExitCode.Success).handleError {
      case noConfig @ AppConfig.NoConfig(help) =>
        if (help.errors.nonEmpty) noConfig.printError()
        else noConfig.printHelp(ConfigFactory.defaultApplication())
      case error: Throwable =>
        error.printStackTrace()
        ExitCode.Error
    }

  def runModules[F[_]: ConcurrentEffect: ContextShift: Timer](
    appConfig:         AppConfig,
    apiConfig:         APIConfig,
    terminationSignal: F[Unit]
  )(
    registry:          CollectorRegistry,
    usersReads:        UsersReads[F],
    usersWrites:       UsersWrites[F],
    discussionsReads:  DiscussionsReads[F],
    discussionsWrites: DiscussionsWrites[F]
  ): F[Unit] =
    Sync[F].delay(logger.info("Initializing services")) >> (
      AppServer
        .asResource(
          appConfig = appConfig,
          apiConfig = apiConfig,
          registry = registry,
          usersReads = usersReads,
          usersWrites = usersWrites,
          discussionsReads = discussionsReads,
          discussionsWrites = discussionsWrites
        )
        .void
        .conditionally(appConfig.runAPI)(orElse = ()),
      usersWrites.runProjector.conditionally(condition = appConfig.runUsersProjections)(orElse = ().pure[F]),
      discussionsWrites.runProjector.conditionally(condition = appConfig.runDiscussionsProjections)(orElse = ().pure[F])
    ).tupled.use { case (_, usersProjector, discussionsProjector) =>
      Sync[F].delay(logger.info("Start projections if needed")) >>
        usersProjector.start >> // run Users projections on a separate thread
        discussionsProjector.start >> // run Users projections on a separate thread
        Sync[F].delay(logger.info("Services initialized")) >>
        terminationSignal >> // here we are blocking until e.g. user press Ctrl+C
        Sync[F].delay(logger.info("Received exit signal"))
    }

  // kudos to Łukasz Byczyński
  private def awaitTerminationSignal[F[_]: Concurrent]: F[Unit] = {
    def handleSignal(signalName: String): F[Unit] = Async[F].async[Unit] { cb =>
      Signal.handle(new Signal(signalName), _ => cb(().asRight[Throwable]))
      ()
    }
    handleSignal("INT").race(handleSignal("TERM")).void
  }

  implicit class ResourceOps[F[_], A](private val resource: Resource[F, A]) extends AnyVal {

    def conditionally(condition: Boolean)(orElse: => A)(implicit F: Applicative[F]): Resource[F, A] =
      if (condition) resource else Resource.pure[F, A](orElse)
  }
}
