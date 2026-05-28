-- Password for both users: admin123
INSERT INTO users (username, nickname, password_hash, role, created_at)
VALUES
    ('admin', '管理员', '$2a$10$.zgynpPJ8sCVJLwI6t.VzOCKozPdxA3b1fGdPVYV3T.o0HjAuVip6', 'ADMIN', NOW()),
    ('demo', 'demo', '$2a$10$.zgynpPJ8sCVJLwI6t.VzOCKozPdxA3b1fGdPVYV3T.o0HjAuVip6', 'USER', NOW());

INSERT INTO categories (name)
VALUES ('技术'), ('生活'), ('随笔');

INSERT INTO tags (name)
VALUES ('Java'), ('Spring Boot'), ('MySQL'), ('日常');
