package com.tzu.myblogcms.article;

import com.tzu.myblogcms.auth.Role;
import com.tzu.myblogcms.auth.User;
import com.tzu.myblogcms.auth.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleLikeService {

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final UserRepository userRepository;

    public ArticleLikeService(ArticleRepository articleRepository,
                              ArticleLikeRepository articleLikeRepository,
                              UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.articleLikeRepository = articleLikeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void toggleLike(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        // 点赞只开放给普通用户，管理员账号只负责内容管理。
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("只有普通用户可以点赞");
        }

        // toggle 操作和 like_count 更新放在同一个事务里，避免列表数字和点赞表不一致。
        articleLikeRepository.findByArticleAndUser(article, user)
                .ifPresentOrElse(
                        like -> {
                            articleLikeRepository.delete(like);
                            article.decrementLikeCount();
                        },
                        () -> {
                            articleLikeRepository.save(new ArticleLike(article, user));
                            article.incrementLikeCount();
                        }
                );
    }

    @Transactional(readOnly = true)
    public boolean isLikedBy(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        Article article = articleRepository.findById(articleId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        return articleLikeRepository.existsByArticleAndUser(article, user);
    }

    @Transactional
    public void deleteLikesByUser(User user) {
        for (ArticleLike like : articleLikeRepository.findByUser(user)) {
            // 删除用户时要先扣减其点过的文章计数，再删除点赞记录。
            like.getArticle().decrementLikeCount();
            articleLikeRepository.delete(like);
        }
    }
}
