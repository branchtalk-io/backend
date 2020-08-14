package io.branchtalk.shared.infrastructure

import cats.effect.{ Async, Blocker, ContextShift, Resource }
import io.branchtalk.shared.infrastructure.DoobieSupport._

trait TestPostgresResources extends TestResourcesHelpers {

  implicit val logger: DoobieSupport.LogHandler = doobieLogger(getClass)

  def postgresConfigResource[F[_]: Async: ContextShift](
    testPostgresConfig: TestPostgresConfig
  ): Resource[F, PostgresConfig] =
    Resource.liftF(generateRandomSuffix[F]).flatMap { randomSuffix =>
      val schemaCreator = Transactor.fromDriverManager[F](
        classOf[org.postgresql.Driver].getName, // driver classname
        testPostgresConfig.url.value.value, // connect URL (driver-specific)
        "postgres", // user
        testPostgresConfig.rootPassword.value.value, // password
        Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
      )

      val cfg      = testPostgresConfig.toPostgresConfig(randomSuffix.toLowerCase)
      val username = Fragment.const(cfg.username.value.value)
      val password = Fragment.const(s"""'${cfg.password.value.value}'""")
      val schema   = Fragment.const(cfg.schema.value.value)

      Resource.make {
        (
          (fr"CREATE USER" ++ username ++ fr"WITH PASSWORD" ++ password).update.run >>
            (fr"CREATE SCHEMA" ++ schema ++ fr"AUTHORIZATION" ++ username).update.run >>
            cfg.pure[ConnectionIO]
        ).transact(schemaCreator)
      } { _ =>
        (
          (fr"DROP SCHEMA IF EXISTS" ++ schema ++ fr"CASCADE").update.run >> (fr"DROP ROLE IF EXISTS" ++ username).update.run
        ).transact(schemaCreator).void
      }
    }

  def postgresDatabaseResource[F[_]: Async: ContextShift](
    testPostgresConfig: TestPostgresConfig
  ): Resource[F, PostgresDatabase] =
    postgresConfigResource[F](testPostgresConfig).map(new PostgresDatabase(_))
}
