-- Enums

CREATE TYPE USER_BAN_TYPE AS ENUM ('for-channel', 'globally');

-- Banned Users

CREATE TABLE bans (
  user_id     UUID          NOT NULL REFERENCES users ON DELETE CASCADE ON UPDATE CASCADE,
  ban_type    USER_BAN_TYPE NOT NULL,
  ban_id      UUID,
  reason      TEXT          NOT NULL
);
