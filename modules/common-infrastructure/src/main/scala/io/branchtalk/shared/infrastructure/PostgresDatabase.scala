package io.branchtalk.shared.infrastructure

import cats.effect.{ Async, Resource, Sync }
import com.zaxxer.hikari.metrics.{ IMetricsTracker, MetricsTrackerFactory, PoolStats }
import com.zaxxer.hikari.metrics.prometheus.PrometheusHistogramMetricsTrackerFactory
import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import io.branchtalk.shared.infrastructure.PostgresDatabase.PrefixedMetricsTrackerFactory
import io.prometheus.client.{ Collector, CollectorRegistry }
import org.flywaydb.core.Flyway

import scala.util.Random

final class PostgresDatabase(config: PostgresConfig) {

  private val randomPrefixLength = 6

  private def flyway[F[_]: Sync] = Sync[F].delay(
    Flyway
      .configure()
      .dataSource(config.url.nonEmptyString.value,
                  config.username.nonEmptyString.value,
                  config.password.nonEmptyString.value
      )
      .schemas(config.schema.nonEmptyString.value)
      .table(s"flyway_${config.domain.nonEmptyString.value}_schema_history")
      .locations(s"db/${config.domain.nonEmptyString.value}/migrations")
      .load()
  )

  def transactor[F[_]: Async](registry: CollectorRegistry): Resource[F, HikariTransactor[F]] =
    for {
      connectEC <- doobie.util.ExecutionContexts.fixedThreadPool[F](config.connectionPool.positiveInt.value)
      xa <- HikariTransactor.initial[F](connectEC)
      _ <- Resource.eval {
        xa.configure { ds =>
          Async[F].delay {
            ds.setMetricsTrackerFactory(
              new PrefixedMetricsTrackerFactory(config.domain.nonEmptyString.value + "_" + LazyList
                                                  .continually(Random.nextPrintableChar())
                                                  .filter(_.isLetter)
                                                  .take(randomPrefixLength)
                                                  .mkString,
                                                registry
              )
            )
            ds.setJdbcUrl(config.url.nonEmptyString.value)
            ds.setUsername(config.username.nonEmptyString.value)
            ds.setPassword(config.password.nonEmptyString.value)
            ds.setMaxLifetime(5 * 60 * 1000)
            ds.setSchema(config.schema.nonEmptyString.value)
          }
        }
      }
      _ <- Resource.eval(migrate[F] >> healthCheck[F](xa))
    } yield xa

  def migrate[F[_]: Sync]: F[Unit] = flyway[F].map(_.migrate()).void

  def healthCheck[F[_]: Sync](xa: Transactor[F]): F[String] = sql"select now()".query[String].unique.transact(xa)
}
object PostgresDatabase {

  // suppress "Collector already registered that provides name: hikaricp_"
  final class NonComplainingCollectorRegistry(impl: CollectorRegistry) extends CollectorRegistry {
    override def register(m: Collector): Unit = try impl.register(m)
    catch { case _: IllegalArgumentException => /* suppressed */ }
    override def unregister(m: Collector): Unit = impl.unregister(m)
    override def clear(): Unit = impl.clear()
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
