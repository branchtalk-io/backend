package io.branchtalk.shared.infrastructure

import cats.effect.{ Async, Blocker, ContextShift, Resource, Sync }
import cats.implicits._
import doobie._
import doobie.implicits._

import scala.util.Random

object TestResources {

  def generateRandomSuffix[F[_]: Sync]: F[String] =
    Sync[F].delay("_" + LazyList.continually(Random.nextPrintableChar()).filterNot(_.isWhitespace).take(6).mkString)

  def databaseConfigResource[F[_]: Async: ContextShift](
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
        val config = testPostgresConfig.toPostgresConfig(randomSuffix)
        (
          sql"""CREATE USER ${config.username.value.value} WITH PASSWORD ${config.password.value.value}""".update.run >>
            sql"""CREATE SCHEMA ${config.schema.value.value} AUTHORIZATION ${config.username.value.value}""".update.run >>
            config.pure[ConnectionIO]
        ).transact(schemaCreator)
      } { config =>
        (
          sql"""DROP ROLE IF EXISTS ${config.username.value.value}""".update.run >>
            sql"""DROP SCHEMA IF EXISTS ${config.schema.value.value}""".update.run
        ).transact(schemaCreator).void
      }
    }

  def databaseResource[F[_]: Async: ContextShift](
    testPostgresConfig: TestPostgresConfig
  ): Resource[F, PostgresDatabase] =
    databaseConfigResource[F](testPostgresConfig).map(new PostgresDatabase(_))

  // TODO: add kafka resource here
}
