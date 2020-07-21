package io.branchtalk.shared.infrastructure

import cats.effect.{ Async, Blocker, ContextShift, Resource }
import doobie._
import doobie.implicits._

trait TestPostgresResources extends TestResourcesHelpers {

  def postgresConfigResource[F[_]: Async: ContextShift](
    testPostgresConfig: TestPostgresConfig
  ): Resource[F, PostgresConfig] =
    Resource.liftF(generateRandomSuffix[F]).flatMap { randomSuffix =>
      val schemaCreator = Transactor.fromDriverManager[F](
        "org.postgresql.Driver", // driver classname
        testPostgresConfig.url.value.value, // connect URL (driver-specific)
        "postgres", // user
        testPostgresConfig.rootPassword.value.value, // password
        Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
      )

      Resource.make {
        val cfg = testPostgresConfig.toPostgresConfig(randomSuffix)
        (
          sql"""CREATE USER ${cfg.username.value.value} WITH PASSWORD ${cfg.password.value.value}""".update.run >>
            sql"""CREATE SCHEMA ${cfg.schema.value.value} AUTHORIZATION ${cfg.username.value.value}""".update.run >>
            cfg.pure[ConnectionIO]
        ).transact(schemaCreator)
      } { cfg =>
        (
          sql"""DROP ROLE IF EXISTS ${cfg.username.value.value}""".update.run >>
            sql"""DROP SCHEMA IF EXISTS ${cfg.schema.value.value}""".update.run
        ).transact(schemaCreator).void
      }
    }

  def postgresDatabaseResource[F[_]: Async: ContextShift](
    testPostgresConfig: TestPostgresConfig
  ): Resource[F, PostgresDatabase] =
    postgresConfigResource[F](testPostgresConfig).map(new PostgresDatabase(_))
}
