-- Full-text search index on posts (Postgres FTS)

ALTER TABLE posts
  ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
      to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content_raw, ''))
    ) STORED;

CREATE INDEX posts_search_idx ON posts USING gin(search_vector);
