CREATE TYPE PASSWORD_ALGORITHM AS ENUM ('bcrypt');

CREATE TYPE SESSION_USAGE_TYPE AS ENUM ('user-session', 'oauth');

CREATE TABLE users (
  id               UUID                     PRIMARY KEY,
  email            TEXT                     NOT NULL UNIQUE,
  username         TEXT                     NOT NULL UNIQUE,
  passwd_algorithm PASSWORD_ALGORITHM       NOT NULL,
  passwd_hash      BYTEA                    NOT NULL,
  passwd_salt      BYTEA                    NOT NULL,
  permissions      JSONB                    NOT NULL,
  created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
  last_modified_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE deleted_users (
  id         UUID                     PRIMARY KEY,
  deleted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE sessions (
  id          UUID                     PRIMARY KEY,
  user_id     UUID                     NOT NULL REFERENCES users ON DELETE CASCADE ON UPDATE CASCADE,
  usage_type  SESSION_USAGE_TYPE       NOT NULL,
  permissions JSONB                    NOT NULL,
  expires_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
