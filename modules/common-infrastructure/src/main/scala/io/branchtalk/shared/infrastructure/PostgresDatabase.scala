package io.branchtalk.shared.infrastructure

import cats.effect.{ Async, Blocker, ContextShift, Resource, Sync }
import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

final class PostgresDatabase(config: PostgresConfig) {

  private def flyway[F[_]: Sync] = Sync[F].delay(
    Flyway
      .configure()
      .dataSource(config.url.nonEmptyString.value,
                  config.username.nonEmptyString.value,
                  config.password.nonEmptyString.value)
      .schemas(config.schema.nonEmptyString.value)
      .table(s"flyway_${config.domain.nonEmptyString.value}_schema_history")
      .locations(s"db/${config.domain.nonEmptyString.value}/migrations")
      .load()
  )

  def transactor[F[_]: Async: ContextShift]: Resource[F, HikariTransactor[F]] =
    for {
      connectEC <- doobie.util.ExecutionContexts.fixedThreadPool[F](config.connectionPool.positiveInt.value)
      transactEC <- doobie.util.ExecutionContexts.cachedThreadPool[F]
      xa <- HikariTransactor.initial[F](connectEC, Blocker.liftExecutionContext(transactEC))
      _ <- Resource.liftF {
        xa.configure { ds =>
          Async[F].delay {
            ds.setJdbcUrl(config.url.nonEmptyString.value)
            ds.setUsername(config.username.nonEmptyString.value)
            ds.setPassword(config.password.nonEmptyString.value)
            ds.setMaxLifetime(5 * 60 * 1000)
            ds.setSchema(config.schema.nonEmptyString.value)
          }
        }
      }
      _ <- Resource.liftF(migrate[F] >> healthCheck[F](xa))
    } yield xa

  def migrate[F[_]: Sync]: F[Unit] = flyway[F].map(_.migrate()).void

  def healthCheck[F[_]: Sync](xa: Transactor[F]): F[String] = sql"select now()".query[String].unique.transact(xa)
}
