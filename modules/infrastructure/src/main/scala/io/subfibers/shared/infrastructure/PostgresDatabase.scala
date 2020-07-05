package io.subfibers.shared.infrastructure

import cats.effect.{ Async, Blocker, ContextShift, Resource, Sync }
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

class PostgresDatabase(config: PostgresConfig) {

  // TODO: add loggers

  private def flyway[F[_]: Sync] = Sync[F].delay(
    Flyway
      .configure()
      .dataSource(config.url.value.value, config.username.value.value, config.password.value.value)
      .schemas(config.schema.value.value)
      .table(s"flyway_${config.domain.value.value}_schema_history")
      .locations(s"db/${config.domain.value.value}/migrations")
      .load()
  )

  def transactor[F[_]: Async: ContextShift]: Resource[F, HikariTransactor[F]] =
    for {
      connectEC <- doobie.util.ExecutionContexts.fixedThreadPool[F](config.connectionPool.value.value)
      transactEC <- doobie.util.ExecutionContexts.cachedThreadPool[F]
      xa <- HikariTransactor.initial[F](connectEC, Blocker.liftExecutionContext(transactEC))
      _ <- Resource.liftF {
        xa.configure { ds =>
          Async[F].delay {
            ds.setJdbcUrl(config.url.value.value)
            ds.setUsername(config.username.value.value)
            ds.setPassword(config.password.value.value)
            ds.setMaxLifetime(5 * 60 * 1000)
            ds.setSchema(config.schema.value.value)
          }
        }
      }
      _ <- Resource.liftF(migrate[F] >> healthCheck[F](xa))
    } yield xa

  def migrate[F[_]: Sync]: F[Unit] = flyway[F].map(_.migrate()).void

  def healthCheck[F[_]: Sync](xa: Transactor[F]): F[String] =
    sql"select now()".query[String].unique.transact(xa)
}
