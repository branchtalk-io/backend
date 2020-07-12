package io.branchtalk.shared.infrastructure

import pureconfig._
import pureconfig.generic.semiauto._

final case class PostgresConfig(
  url:              DatabaseURL,
  username:         DatabaseUsername,
  password:         DatabasePassword,
  schema:           DatabaseSchema,
  domain:           DatabaseDomain,
  connectionPool:   DatabaseConnectionPool,
  migrationOnStart: DatabaseMigrationOnStart
)
object PostgresConfig {

  implicit val configReader: ConfigReader[PostgresConfig] = deriveReader[PostgresConfig]
}
