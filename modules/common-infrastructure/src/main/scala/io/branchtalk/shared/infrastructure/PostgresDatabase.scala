package io.branchtalk.shared.infrastructure

import cats.Show
import cats.effect.{ Async, Resource, Sync }
import com.zaxxer.hikari.metrics.{ IMetricsTracker, MetricsTrackerFactory, PoolStats }
import com.zaxxer.hikari.metrics.prometheus.PrometheusHistogramMetricsTrackerFactory
import doobie.*
import doobie.implicits.*
import doobie.hikari.HikariTransactor
import io.branchtalk.logging.Logger
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.infrastructure.PureconfigSupport.*
import io.branchtalk.shared.model.*
import io.prometheus.client.{ Collector, CollectorRegistry }
import org.flywaydb.core.Flyway

import scala.util.Random

final class PostgresDatabase(config: PostgresDatabase.Config) {

  private val randomPrefixLength = 6

  private def flyway[F[_]: Sync] = Sync[F].delay(
    Flyway
      .configure()
      .dataSource(config.url.unwrap, config.username.unwrap, config.password.unwrap)
      .schemas(config.schema.unwrap)
      .table(s"flyway_${config.domain.unwrap}_schema_history")
      .locations(s"db/${config.domain.unwrap}/migrations")
      .load()
  )

  def transactor[F[_]: Async](logger: Logger[F], registry: CollectorRegistry): Resource[F, HikariTransactor[F]] =
    for {
      connectEC <- doobie.util.ExecutionContexts.fixedThreadPool[F](config.connectionPool.unwrap)
      xa <- HikariTransactor.initial[F](connectEC, logHandler = Some(doobieLogger(logger)))
      _ <- Resource.eval {
        xa.configure { ds =>
          Async[F].delay {
            ds.setMetricsTrackerFactory(
              new PostgresDatabase.PrefixedMetricsTrackerFactory(
                config.domain.unwrap + "_" + LazyList
                  .continually(Random.nextPrintableChar())
                  .filter(_.isLetter)
                  .take(randomPrefixLength)
                  .mkString,
                registry
              )
            )
            ds.setJdbcUrl(config.url.unwrap)
            ds.setUsername(config.username.unwrap)
            ds.setPassword(config.password.unwrap)
            ds.setMaxLifetime(5 * 60 * 1000)
            ds.setSchema(config.schema.unwrap)
          }
        }
      }
      _ <- Resource.eval(migrate[F] >> healthCheck[F](xa))
    } yield xa

  def migrate[F[_]: Sync]: F[Unit] = flyway[F].map(_.migrate()).void

  def healthCheck[F[_]: Sync](xa: Transactor[F]): F[String] =
    sql"select now()".queryWithLabel[String]("DB Health Check").unique.transact(xa)
}
object PostgresDatabase {

  type URL = URL.Type
  object URL extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(url: URL): Some[String] = Some(url.unwrap)

    given ConfigReader[URL] = ConfigReader[String].emapString("URL")(make)
    given Show[URL]         = unsafeMakeF[Show](Show[String])
  }

  type Username = Username.Type
  object Username extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(username: Username): Some[String] = Some(username.unwrap)

    given ConfigReader[Username] = ConfigReader[String].emapString("Username")(make)
    given Show[Username]         = unsafeMakeF[Show](Show[String])
  }

  // not a newtype to override toString
  final case class Password private (unwrap: String) {
    override def toString: String = "[PASSWORD]"
  }
  object Password {
    def parse(string: String): Either[String, Password] =
      if string.nonEmpty then Right(Password(string)) else Left("Password cannot be empty")

    given ConfigReader[Password] = ConfigReader[String].emapString("Password")(parse)
    given Show[Password]         = _ => "PASSWORD"
  }

  type Schema = Schema.Type
  object Schema extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(domainName: Schema): Some[String] = Some(domainName.unwrap)

    given ConfigReader[Schema] = ConfigReader[String].emapString("Schema")(make)
    given Show[Schema]         = unsafeMakeF[Show](Show[String])
  }

  type Domain = Domain.Type
  object Domain extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(domain: Domain): Some[String] = Some(domain.unwrap)

    given ConfigReader[Domain] = ConfigReader[String].emapString("Domain")(make)
    given Show[Domain]         = unsafeMakeF[Show](Show[String])
  }

  type ConnectionPool = ConnectionPool.Type
  object ConnectionPool extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input > 0

    def unapply(connectionPool: ConnectionPool): Some[Int] = Some(connectionPool.unwrap)

    given ConfigReader[ConnectionPool] = ConfigReader[Int].emapString("ConnectionPool")(make)
    given Show[ConnectionPool]         = unsafeMakeF[Show](Show[Int])
  }

  type MigrationOnStart = MigrationOnStart.Type
  object MigrationOnStart extends Newtype[Boolean] {

    def unapply(migrationOnStart: MigrationOnStart): Some[Boolean] = Some(migrationOnStart.unwrap)

    given ConfigReader[MigrationOnStart] = unsafeMakeF[ConfigReader](ConfigReader[Boolean])
    given Show[MigrationOnStart]         = unsafeMakeF[Show](Show[Boolean])
  }

  final case class Config(
    url:              URL,
    username:         Username,
    password:         Password,
    schema:           Schema,
    domain:           Domain,
    connectionPool:   ConnectionPool,
    migrationOnStart: MigrationOnStart
  ) derives ConfigReader,
        ShowPretty

  // suppress "Collector already registered that provides name: hikaricp_"
  final private class NonComplainingCollectorRegistry(impl: CollectorRegistry) extends CollectorRegistry {
    override def register(m: Collector): Unit = try impl.register(m)
    catch { case _: IllegalArgumentException => /* suppressed */ }
    override def unregister(m: Collector): Unit = impl.unregister(m)
    override def clear():                  Unit = impl.clear()
    override def metricFamilySamples(): java.util.Enumeration[Collector.MetricFamilySamples] =
      impl.metricFamilySamples()
    override def filteredMetricFamilySamples(
      includedNames: java.util.Set[String]
    ): java.util.Enumeration[Collector.MetricFamilySamples] =
      impl.filteredMetricFamilySamples(includedNames)
    override def getSampleValue(name: String): java.lang.Double = impl.getSampleValue(name)
    override def getSampleValue(name: String, labelNames: Array[String], labelValues: Array[String]): java.lang.Double =
      impl.getSampleValue(name, labelNames, labelValues)
  }

  // solves the issue of name clashing when registering Hikari collectors
  final class PrefixedMetricsTrackerFactory(prefix: String, registry: CollectorRegistry) extends MetricsTrackerFactory {

    private val impl = new PrometheusHistogramMetricsTrackerFactory(new NonComplainingCollectorRegistry(registry))

    override def create(poolName: String, poolStats: PoolStats): IMetricsTracker =
      impl.create(s"${prefix}_$poolName", poolStats)
  }
}
