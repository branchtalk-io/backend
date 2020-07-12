package io.branchtalk.shared.infrastructure

import io.scalaland.catnip.Semi
import pureconfig._

@Semi(ConfigReader) final case class PostgresConfig(
  url:              DatabaseURL,
  username:         DatabaseUsername,
  password:         DatabasePassword,
  schema:           DatabaseSchema,
  domain:           DatabaseDomain,
  connectionPool:   DatabaseConnectionPool,
  migrationOnStart: DatabaseMigrationOnStart
)
