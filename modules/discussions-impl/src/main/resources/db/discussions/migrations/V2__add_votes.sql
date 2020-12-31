-- Enums

CREATE TYPE VOTE_TYPE AS ENUM ('upvote', 'downvote');

-- Votes

CREATE TABLE post_votes (
  post_id  UUID      NOT NULL REFERENCES posts,
  voter_id UUID      NOT NULL,
  vote     VOTE_TYPE NOT NULL,
  PRIMARY KEY (post_id, voter_id)
);

CREATE TABLE comment_votes (
  comment_id UUID      NOT NULL REFERENCES comments,
  voter_id   UUID      NOT NULL,
  vote       VOTE_TYPE NOT NULL,
  PRIMARY KEY (comment_id, voter_id)
);

-- Posts

ALTER TABLE posts
  ADD COLUMN upvotes_nr          INT NOT NULL DEFAULT 0 CHECK (upvotes_nr >= 0),
  ADD COLUMN downvotes_nr        INT NOT NULL DEFAULT 0 CHECK (downvotes_nr >= 0),
  ADD COLUMN total_score         INT NOT NULL DEFAULT 0 CHECK (total_score = upvotes_nr - downvotes_nr),
  ADD COLUMN controversial_score INT NOT NULL DEFAULT 0 CHECK (controversial_score = LEAST(upvotes_nr, downvotes_nr));

-- Comments

ALTER TABLE comments
  ADD COLUMN upvotes_nr          INT NOT NULL DEFAULT 0 CHECK (upvotes_nr >= 0),
  ADD COLUMN downvotes_nr        INT NOT NULL DEFAULT 0 CHECK (downvotes_nr >= 0),
  ADD COLUMN total_score         INT NOT NULL DEFAULT 0 CHECK (total_score = upvotes_nr - downvotes_nr),
  ADD COLUMN controversial_score INT NOT NULL DEFAULT 0 CHECK (controversial_score = LEAST(upvotes_nr, downvotes_nr));
