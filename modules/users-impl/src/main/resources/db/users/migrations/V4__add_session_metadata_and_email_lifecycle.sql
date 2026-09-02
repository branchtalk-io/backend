-- Issue #7: Session metadata (IP address and User-Agent)
ALTER TABLE sessions ADD COLUMN ip_address TEXT;
ALTER TABLE sessions ADD COLUMN user_agent TEXT;

-- Issue #8: Email confirmation lifecycle
ALTER TABLE users ADD COLUMN email_status TEXT NOT NULL DEFAULT 'New';
ALTER TABLE users ADD COLUMN pending_email TEXT;
ALTER TABLE users ADD COLUMN confirmation_token TEXT;
