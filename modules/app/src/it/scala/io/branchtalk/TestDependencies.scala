package io.branchtalk

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import com.softwaremill.macwire.wire
import io.branchtalk.discussions.{ DiscussionsModule, DiscussionsReads, DiscussionsWrites, TestDiscussionsConfig }
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.users.{ TestUsersConfig, UsersModule, UsersReads, UsersWrites }

final case class TestDependencies[F[_]](
  usersReads:        UsersReads[F],
  usersWrites:       UsersWrites[F],
  discussionsReads:  DiscussionsReads[F],
  discussionsWrites: DiscussionsWrites[F]
)
object TestDependencies {

  def resources[F[_]: ConcurrentEffect: ContextShift: Timer](
    implicit uuidGenerator: UUIDGenerator
  ): Resource[F, TestDependencies[F]] =
    for {
      usersConfig <- TestUsersConfig.loadDomainConfig[F]
      usersReads <- UsersModule.reads[F](usersConfig)
      usersWrites <- UsersModule.writes[F](usersConfig)
      discussionsConfig <- TestDiscussionsConfig.loadDomainConfig[F]
      discussionsReads <- DiscussionsModule.reads[F](discussionsConfig)
      discussionsWrites <- DiscussionsModule.writes[F](discussionsConfig)
    } yield wire[TestDependencies[F]]
}
