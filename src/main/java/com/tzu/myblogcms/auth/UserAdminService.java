package com.tzu.myblogcms.auth;

import com.tzu.myblogcms.article.Article;
import com.tzu.myblogcms.article.ArticleLikeService;
import com.tzu.myblogcms.article.ArticleRepository;
import com.tzu.myblogcms.comment.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final ArticleLikeService articleLikeService;

    public UserAdminService(UserRepository userRepository,
                            ArticleRepository articleRepository,
                            CommentRepository commentRepository,
                            ArticleLikeService articleLikeService) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.articleLikeService = articleLikeService;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        articleLikeService.deleteLikesByUser(user);
        commentRepository.deleteByAuthor(user);
        for (Article article : articleRepository.findByAuthor(user)) {
            articleRepository.delete(article);
        }
        userRepository.delete(user);
    }
}
