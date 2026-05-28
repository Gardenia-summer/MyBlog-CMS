ALTER TABLE users ADD COLUMN bio VARCHAR(300) NULL AFTER avatar_url;

UPDATE users
SET bio = '这个用户很懒~什么都没有写~'
WHERE role = 'USER'
  AND (bio IS NULL OR bio = '');
