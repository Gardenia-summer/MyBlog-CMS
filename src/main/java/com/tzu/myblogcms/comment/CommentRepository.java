package com.tzu.myblogcms.comment;

import com.tzu.myblogcms.article.Article;
import com.tzu.myblogcms.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByArticleOrderByCreatedAtAsc(Article article);

    @EntityGraph(attributePaths = {"article", "article.author", "author"})
    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    void deleteByAuthor(User author);
}
