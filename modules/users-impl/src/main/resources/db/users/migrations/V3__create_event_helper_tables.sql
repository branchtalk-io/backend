-- enums

CREATE TYPE DATA_ENCRYPTION_ALGORITHM AS ENUM ('blowfish');

-- Reservations made by commands before events' projections make the actual change

CREATE TABLE reserved_emails (
  email TEXT PRIMARY KEY
);

CREATE TABLE reserved_usernames (
  username TEXT PRIMARY KEY
);

-- GDPR data and other sensitive data encryption keys

CREATE TABLE sensitive_data_keys (
  user_id       UUID                      PRIMARY KEY,
  key_value     BYTEA                     NOT NULL,
  enc_algorithm DATA_ENCRYPTION_ALGORITHM NOT NULL
);
