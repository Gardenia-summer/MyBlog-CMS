ALTER TABLE articles ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER updated_at;

CREATE TABLE article_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_article_likes_article_user UNIQUE (article_id, user_id),
    CONSTRAINT fk_article_likes_article FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    CONSTRAINT fk_article_likes_user FOREIGN KEY (user_id) REFERENCES users(id)
);
