package io.branchtalk.shared.infrastructure

final case class PostgresConfig(
  url:              DatabaseURL,
  username:         DatabaseUsername,
  password:         DatabasePassword,
  schema:           DatabaseSchema,
  domain:           DatabaseDomain,
  connectionPool:   DatabaseConnectionPool,
  migrationOnStart: DatabaseMigrationOnStart
)
