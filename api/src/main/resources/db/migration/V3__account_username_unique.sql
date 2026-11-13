-- Usernames are unique whatever their case; the service checks first, the index settles a race.
CREATE UNIQUE INDEX account_username_lower_idx ON account (lower(username));
