package com.tzu.myblogcms.article;

import com.tzu.myblogcms.auth.User;
import com.tzu.myblogcms.auth.UserRepository;
import com.tzu.myblogcms.category.Category;
import com.tzu.myblogcms.category.CategoryService;
import com.tzu.myblogcms.tag.Tag;
import com.tzu.myblogcms.tag.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    public ArticleService(ArticleRepository articleRepository,
                          UserRepository userRepository,
                          CategoryService categoryService,
                          TagService tagService) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    public List<Article> listArticles() {
        return articleRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Article requireArticle(Long id) {
        return articleRepository.findById(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Page<Article> searchArticles(String keyword, Long categoryId, Long tagId, Pageable pageable) {
        String cleanKeyword = keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
        List<Article> filtered = articleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(article -> categoryId == null || article.getCategory().getId().equals(categoryId))
                .filter(article -> tagId == null || article.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)))
                .filter(article -> cleanKeyword == null || matchesKeyword(article, cleanKeyword))
                .toList();
        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(new ArrayList<>(filtered.subList(start, end)), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public Page<Article> listMine(Long authorId, Pageable pageable) {
        User author = userRepository.findById(authorId).orElseThrow();
        return articleRepository.findByAuthorOrderByCreatedAtDesc(author, pageable);
    }

    @Transactional
    public Article createArticle(String title, String content) {
        return articleRepository.save(new Article(title.trim(), content.trim()));
    }

    @Transactional
    public Article createArticle(ArticleForm form, Long authorId) {
        validate(form);
        User author = userRepository.findById(authorId).orElseThrow();
        Category category = categoryService.requireCategory(form.getCategoryId());
        Set<Tag> tags = tagService.findAllByIds(form.getTagIds());
        return articleRepository.save(new Article(form.getTitle().trim(), form.getContent().trim(), author, category, tags));
    }

    @Transactional
    public void updateOwnArticle(Long articleId, ArticleForm form, Long authorId) {
        Article article = requireArticle(articleId);
        if (!article.getAuthor().getId().equals(authorId)) {
            throw new IllegalArgumentException("Only the author can edit this article");
        }
        updateArticle(articleId, form);
    }

    @Transactional
    public void updateArticle(Long articleId, ArticleForm form) {
        validate(form);
        Article article = requireArticle(articleId);
        Category category = categoryService.requireCategory(form.getCategoryId());
        Set<Tag> tags = tagService.findAllByIds(form.getTagIds());
        article.update(form.getTitle().trim(), form.getContent().trim(), category, tags);
    }

    @Transactional
    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }

    @Transactional
    public void deleteOwnArticle(Long id, Long authorId) {
        Article article = requireArticle(id);
        if (!article.getAuthor().getId().equals(authorId)) {
            throw new IllegalArgumentException("Only the author can delete this article");
        }
        articleRepository.delete(article);
    }

    public ArticleForm toForm(Article article) {
        ArticleForm form = new ArticleForm();
        form.setTitle(article.getTitle());
        form.setContent(article.getContent());
        form.setCategoryId(article.getCategory().getId());
        form.setTagIds(article.getTags().stream().map(Tag::getId).toList());
        return form;
    }

    private void validate(ArticleForm form) {
        if (form == null || isBlank(form.getTitle()) || isBlank(form.getContent()) || form.getCategoryId() == null) {
            throw new IllegalArgumentException("Title, content, and category are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean matchesKeyword(Article article, String keyword) {
        String needle = keyword.toLowerCase();
        return contains(article.getTitle(), needle)
                || contains(article.getContent(), needle)
                || contains(article.getAuthor().getUsername(), needle)
                || contains(article.getCategory().getName(), needle)
                || article.getTags().stream().anyMatch(tag -> contains(tag.getName(), needle));
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
