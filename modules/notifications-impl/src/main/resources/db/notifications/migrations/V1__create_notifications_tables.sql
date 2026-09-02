-- Enums

CREATE TYPE notification_kind AS ENUM ('postreply', 'commentreply', 'newpostinchannel');

-- Notifications

CREATE TABLE notifications (
  id                UUID                     PRIMARY KEY,
  recipient_id      UUID                     NOT NULL,
  kind              notification_kind        NOT NULL,
  source_post_id    UUID,
  source_comment_id UUID,
  source_user_id    UUID,
  message           TEXT                     NOT NULL,
  created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
  read_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX notifications_recipient_time_idx ON notifications (recipient_id, created_at DESC);
CREATE INDEX notifications_recipient_unread_idx ON notifications (recipient_id) WHERE read_at IS NULL;
