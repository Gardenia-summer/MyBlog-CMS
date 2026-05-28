package com.tzu.myblogcms.tag;

import com.tzu.myblogcms.article.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;

    public TagService(TagRepository tagRepository, ArticleRepository articleRepository) {
        this.tagRepository = tagRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> listTags() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Set<Tag> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(tagRepository.findAllById(ids));
    }

    @Transactional(readOnly = true)
    public Tag requireTag(Long id) {
        return tagRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Tag create(String name) {
        return tagRepository.save(new Tag(clean(name)));
    }

    @Transactional
    public void delete(Long id) {
        if (articleRepository.countByTags_Id(id) > 0) {
            throw new IllegalArgumentException("已有文章使用该标签，无法删除");
        }
        tagRepository.deleteById(id);
    }

    private String clean(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        return name.trim();
    }
}
