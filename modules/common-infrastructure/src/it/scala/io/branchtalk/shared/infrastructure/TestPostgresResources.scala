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
        driver = classOf[org.postgresql.Driver].getName, // driver classname
        url = testPostgresConfig.url.nonEmptyString.value, // connect URL (driver-specific)
        user = "postgres", // user
        pass = testPostgresConfig.rootPassword.nonEmptyString.value, // password
        blocker = Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
      )

      val cfg      = testPostgresConfig.toPostgresConfig(randomSuffix.toLowerCase)
      val username = Fragment.const(cfg.username.nonEmptyString.value)
      val password = Fragment.const(s"""'${cfg.password.nonEmptyString.value}'""")
      val schema   = Fragment.const(cfg.schema.nonEmptyString.value)

      val createUser   = (fr"CREATE USER" ++ username ++ fr"WITH PASSWORD" ++ password).update.run
      val createSchema = (fr"CREATE SCHEMA" ++ schema ++ fr"AUTHORIZATION" ++ username).update.run

      val dropSchema = (fr"DROP SCHEMA IF EXISTS" ++ schema ++ fr"CASCADE").update.run
      val dropUser   = (fr"DROP ROLE IF EXISTS" ++ username).update.run

      Resource.make {
        (createUser >> createSchema >> cfg.pure[ConnectionIO]).transact(schemaCreator)
      }(_ => (dropSchema >> dropUser).transact(schemaCreator).void)
    }

  def postgresDatabaseResource[F[_]: Async: ContextShift](
    testPostgresConfig: TestPostgresConfig
  ): Resource[F, PostgresDatabase] =
    postgresConfigResource[F](testPostgresConfig).map(new PostgresDatabase(_))
}
