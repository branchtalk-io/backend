-- Enums

CREATE TYPE VOTE_TYPE AS ENUM ('upvote', 'downvote');

-- Votes

CREATE TABLE post_votes (
  post_id UUID      NOT NULL REFERENCES posts,
  user_id UUID      NOT NULL,
  vote    VOTE_TYPE NOT NULL,
  PRIMARY KEY (post_id, user_id)
);

CREATE TABLE comment_votes (
  comment_id UUID      NOT NULL REFERENCES comments,
  user_id    UUID      NOT NULL,
  vote       VOTE_TYPE NOT NULL,
  PRIMARY KEY (comment_id, user_id)
);

-- Posts

ALTER TABLE posts
  ADD COLUMN upvotes_nr          INT NOT NULL DEFAULT 0 CHECK (upvotes_nr >= 0),
      COLUMN downvotes_nr        INT NOT NULL DEFAULT 0 CHECK (downvotes_nr >= 0),
      COLUMN total_score         INT NOT NULL DEFAULT 0,
      COLUMN controversial_score INT NOT NULL DEFAULT 0;

-- Comments

ALTER TABLE comments
  ADD COLUMN upvotes_nr          INT NOT NULL DEFAULT 0 CHECK (upvotes_nr >= 0),
      COLUMN downvotes_nr        INT NOT NULL DEFAULT 0 CHECK (downvotes_nr >= 0),
      COLUMN total_score         INT NOT NULL DEFAULT 0,
      COLUMN controversial_score INT NOT NULL DEFAULT 0;
