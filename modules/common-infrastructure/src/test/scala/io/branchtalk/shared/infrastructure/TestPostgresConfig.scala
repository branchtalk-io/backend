package io.branchtalk.shared.infrastructure

import io.scalaland.chimney.dsl.*
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }

final case class TestPostgresConfig(
  url:            PostgresDatabase.URL,
  rootPassword:   PostgresDatabase.Password,
  usernamePrefix: PostgresDatabase.Username,
  password:       PostgresDatabase.Password,
  schemaPrefix:   PostgresDatabase.Schema,
  domain:         PostgresDatabase.Domain,
  connectionPool: PostgresDatabase.ConnectionPool
) derives ConfigReader {

  def username(generatedSuffix: String): PostgresDatabase.Username =
    PostgresDatabase.Username.unsafeMake(usernamePrefix.unwrap + generatedSuffix)
  def schema(generatedSuffix: String): PostgresDatabase.Schema =
    PostgresDatabase.Schema.unsafeMake(schemaPrefix.unwrap + generatedSuffix)
  def migrationOnStart: PostgresDatabase.MigrationOnStart = PostgresDatabase.MigrationOnStart(true)

  def toPostgresConfig(generatedSuffix: String): PostgresDatabase.Config =
    this
      .into[PostgresDatabase.Config]
      .withFieldConst(_.username, username(generatedSuffix))
      .withFieldConst(_.schema, schema(generatedSuffix))
      .withFieldConst(_.migrationOnStart, migrationOnStart)
      .transform
}
