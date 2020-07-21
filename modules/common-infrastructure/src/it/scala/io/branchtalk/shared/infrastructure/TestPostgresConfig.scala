package io.branchtalk.shared.infrastructure

import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._
import pureconfig.ConfigReader

@Semi(ConfigReader) final case class TestPostgresConfig(
  url:            DatabaseURL,
  rootPassword:   DatabasePassword,
  usernamePrefix: DatabaseUsername,
  password:       DatabasePassword,
  schemaPrefix:   DatabaseSchema,
  domainPrefix:   DatabaseDomain,
  connectionPool: DatabaseConnectionPool
) {

  def username(generatedSuffix: String): DatabaseUsername =
    DatabaseUsername(refineV[NonEmpty](usernamePrefix.value.value + generatedSuffix).getOrElse(???))
  def schema(generatedSuffix: String): DatabaseSchema =
    DatabaseSchema(refineV[NonEmpty](schemaPrefix.value.value + generatedSuffix).getOrElse(???))
  def domain(generatedSuffix: String): DatabaseDomain =
    DatabaseDomain(refineV[NonEmpty](domainPrefix.value.value + generatedSuffix).getOrElse(???))
  def migrationOnStart: DatabaseMigrationOnStart = DatabaseMigrationOnStart(true)

  def toPostgresConfig(generatedSuffix: String): PostgresConfig =
    this
      .into[PostgresConfig]
      .withFieldConst(_.username, username(generatedSuffix))
      .withFieldConst(_.schema, schema(generatedSuffix))
      .withFieldConst(_.domain, domain(generatedSuffix))
      .withFieldConst(_.migrationOnStart, migrationOnStart)
      .transform
}
