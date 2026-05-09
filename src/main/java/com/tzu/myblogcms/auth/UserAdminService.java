package com.tzu.myblogcms.auth;

import com.tzu.myblogcms.article.Article;
import com.tzu.myblogcms.article.ArticleRepository;
import com.tzu.myblogcms.comment.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    public UserAdminService(UserRepository userRepository,
                            ArticleRepository articleRepository,
                            CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        commentRepository.deleteByAuthor(user);
        for (Article article : articleRepository.findByAuthor(user)) {
            articleRepository.delete(article);
        }
        userRepository.delete(user);
    }
}
