package io.branchtalk.shared.infrastructure

import cats.effect.{ Async, Resource }
import io.branchtalk.logging.Logger
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }

trait TestPostgresResources extends TestResourcesHelpers {

  def postgresConfigResource[F[_]: Async](
    testPostgresConfig: TestPostgresConfig
  ): Resource[F, PostgresDatabase.Config] =
    Resource.eval(generateRandomSuffix[F]).flatMap { randomSuffix =>
      val schemaCreator = Transactor.fromDriverManager[F](
        driver = classOf[org.postgresql.Driver].getName, // driver classname
        url = testPostgresConfig.url.unwrap, // connect URL (driver-specific)
        user = "postgres", // user
        password = testPostgresConfig.rootPassword.unwrap, // password
        logHandler = Some(doobieLogger[F](Logger.getLoggerFromClass[F](getClass)))
      )

      val cfg      = testPostgresConfig.toPostgresConfig(randomSuffix.toLowerCase)
      val username = Fragment.const(cfg.username.unwrap)
      val password = Fragment.const(s"""'${cfg.password.unwrap}'""")
      val schema   = Fragment.const(cfg.schema.unwrap)

      val createUser = sql"CREATE USER $username WITH PASSWORD $password"
        .updateWithLabel(s"Create test Postgres user ${cfg.username.unwrap}")
        .run
      val createSchema = sql"CREATE SCHEMA $schema AUTHORIZATION $username"
        .updateWithLabel(s"Create test Postgres schema ${cfg.schema.unwrap}")
        .run

      val dropSchema = sql"DROP SCHEMA IF EXISTS $schema CASCADE"
        .updateWithLabel(s"Drop test Postgres schema ${cfg.schema.unwrap}")
        .run
      val dropUser =
        sql"DROP ROLE IF EXISTS $username".updateWithLabel(s"Drop test Postgres role ${cfg.username.unwrap}").run

      Resource.make {
        (createUser >> createSchema >> cfg.pure[ConnectionIO]).transact(schemaCreator)
      }(_ => (dropSchema >> dropUser).transact(schemaCreator).void)
    }

  def postgresDatabaseResource[F[_]: Async](
    testPostgresConfig: TestPostgresConfig
  ): Resource[F, PostgresDatabase] =
    postgresConfigResource[F](testPostgresConfig).map(new PostgresDatabase(_))
}
