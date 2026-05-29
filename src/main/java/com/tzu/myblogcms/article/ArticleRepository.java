package com.tzu.myblogcms.article;

import com.tzu.myblogcms.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Override
    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Optional<Article> findById(Long id);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    List<Article> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    List<Article> findAllByOrderByLikeCountDescCreatedAtAsc();

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findByAuthorOrderByLikeCountDescCreatedAtAsc(User author, Pageable pageable);

    List<Article> findByAuthor(User author);

    long countByCategory_Id(Long categoryId);

    long countByTags_Id(Long tagId);
}
