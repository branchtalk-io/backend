-- Full-text search index on comments (Postgres FTS)

ALTER TABLE comments
  ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
      to_tsvector('english', coalesce(content, ''))
    ) STORED;

CREATE INDEX comments_search_idx ON comments USING gin(search_vector);
