ALTER TABLE users ADD COLUMN nickname VARCHAR(80) NULL AFTER username;
UPDATE users SET nickname = username WHERE nickname IS NULL OR nickname = '';
ALTER TABLE users MODIFY COLUMN nickname VARCHAR(80) NOT NULL;
