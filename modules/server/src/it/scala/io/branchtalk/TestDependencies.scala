package io.branchtalk

import cats.effect.std.Dispatcher
import cats.effect.{ Async, Resource }
import com.softwaremill.macwire.wire
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites, TestDiscussionsConfig }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.model.UUIDGenerator
import io.branchtalk.users.{ TestUsersConfig, UsersModule, UsersReads, UsersWrites }
import io.prometheus.client.CollectorRegistry

import scala.annotation.nowarn

final case class TestDependencies[F[_]](
  usersReads:        UsersReads[F],
  usersWrites:       UsersWrites[F],
  discussionsReads:  DiscussionsReads[F],
  discussionsWrites: DiscussionsWrites[F]
)
object TestDependencies {

  @nowarn("cat=unused") // macwire
  def resources[F[_]: Async: MDC](registry: CollectorRegistry)(implicit
    uuidGenerator: UUIDGenerator
  ): Resource[F, TestDependencies[F]] =
    for {
      implicit0(dispatcher: Dispatcher[F]) <- Dispatcher[F]
      usersConfig <- TestUsersConfig.loadDomainConfig[F]
      usersReads <- UsersModule.reads[F](usersConfig, registry)
      usersWrites <- UsersModule.writes[F](usersConfig, registry)
      discussionsConfig <- TestDiscussionsConfig.loadDomainConfig[F]
      discussionsReads <- DiscussionsModule.reads[F](discussionsConfig, registry)
      discussionsWrites <- DiscussionsModule.writes[F](discussionsConfig, registry)
    } yield wire[TestDependencies[F]]
}
