-- Enums

CREATE TYPE USER_BAN_TYPE AS ENUM ('per-channel', 'globally');

-- Banned Users

CREATE TABLE banned_users (
  user_id     UUID          NOT NULL REFERENCES users ON DELETE CASCADE ON UPDATE CASCADE,
  ban_type    USER_BAN_TYPE NOT NULL,
  channel_ids UUID[]        NOT NULL
);
