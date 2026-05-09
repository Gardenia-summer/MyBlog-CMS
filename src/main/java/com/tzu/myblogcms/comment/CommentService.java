package com.tzu.myblogcms.comment;

import com.tzu.myblogcms.article.Article;
import com.tzu.myblogcms.article.ArticleService;
import com.tzu.myblogcms.auth.User;
import com.tzu.myblogcms.auth.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleService articleService;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, ArticleService articleService, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.articleService = articleService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Comment> listByArticle(Article article) {
        return commentRepository.findByArticleOrderByCreatedAtAsc(article);
    }

    @Transactional(readOnly = true)
    public Page<Comment> listComments(Pageable pageable) {
        return commentRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public Comment create(Long articleId, Long authorId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment is required");
        }
        Article article = articleService.requireArticle(articleId);
        User author = userRepository.findById(authorId).orElseThrow();
        return commentRepository.save(new Comment(article, author, content.trim()));
    }

    @Transactional
    public void delete(Long id) {
        commentRepository.deleteById(id);
    }
}
