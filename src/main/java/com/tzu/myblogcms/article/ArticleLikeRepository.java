package com.tzu.myblogcms.article;

import com.tzu.myblogcms.auth.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    Optional<ArticleLike> findByArticleAndUser(Article article, User user);

    boolean existsByArticleAndUser(Article article, User user);

    long countByArticle_Id(Long articleId);

    long countByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"article"})
    List<ArticleLike> findByUser(User user);
}
