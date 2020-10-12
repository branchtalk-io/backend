CREATE TYPE POST_CONTENT_TYPE AS ENUM ('url', 'text');

CREATE TABLE channels (
  id               UUID                     PRIMARY KEY,
  url_name         TEXT                     NOT NULL,
  name             TEXT                     NOT NULL,
  description      TEXT,
  created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
  last_modified_at TIMESTAMP WITH TIME ZONE,
  deleted          BOOLEAN                  NOT NULL DEFAULT FALSE
);

CREATE TABLE posts (
  id               UUID                     PRIMARY KEY,
  author_id        UUID                     NOT NULL,
  channel_id       UUID                     NOT NULL REFERENCES channels ON DELETE CASCADE ON UPDATE CASCADE,
  url_title        TEXT                     NOT NULL,
  title            TEXT                     NOT NULL,
  content_type     POST_CONTENT_TYPE        NOT NULL,
  content_raw      TEXT                     NOT NULL,
  created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
  last_modified_at TIMESTAMP WITH TIME ZONE,
  deleted          BOOLEAN                  NOT NULL DEFAULT FALSE
);

CREATE INDEX posts_channel_time_idx ON posts (channel_id, created_at);

CREATE TABLE comments (
  id               UUID                     PRIMARY KEY,
  author_id        UUID                     NOT NULL,
  post_id          UUID                     NOT NULL REFERENCES posts ON DELETE CASCADE ON UPDATE CASCADE,
  content          TEXT                     NOT NULL,
  reply_to         UUID                     REFERENCES comments ON DELETE CASCADE ON UPDATE CASCADE,
  nesting_level    SMALLINT                 NOT NULL,
  created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
  last_modified_at TIMESTAMP WITH TIME ZONE,
  deleted          BOOLEAN                  NOT NULL DEFAULT FALSE
);

CREATE INDEX comments_post_time_idx ON comments (post_id, created_at);

CREATE TABLE SUBSCRIPTIONS (
  subscriber_id     UUID   NOT NULL UNIQUE,
  subscriptions_ids UUID[] NOT NULL
);

CREATE FUNCTION array_diff(array1 anyarray, array2 anyarray) RETURNS anyarray $$
  SELECT COALESCE(array_agg(elem), '{}') FROM unnest(array1) elem WHERE elem <> all(array2);
$$ LANGUAGE SQL IMMUTABLE;

CREATE FUNCTION array_distinct(anyarray) RETURNS anyarray AS $$
  SELECT array_agg(DISTINCT x) FROM unnest($1) t(x);
$$ LANGUAGE SQL IMMUTABLE;


-- TODO: comment store channel ID to allow easier partitioning
-- TODO: add upvotes/downvotes/total to posts and comments, create tables to trace them
