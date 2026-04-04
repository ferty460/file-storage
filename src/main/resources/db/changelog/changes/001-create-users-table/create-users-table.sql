CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_name ON users(name);

COMMENT ON TABLE users IS 'Table with information about users';
COMMENT ON COLUMN users.id IS 'Unique user ID';
COMMENT ON COLUMN users.name IS 'The user''s name (must be unique)';
COMMENT ON COLUMN users.password IS 'The hash of the user''s password (must be stored in encrypted form)';