package com.tzu.myblogcms.category;

import com.tzu.myblogcms.article.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    public CategoryService(CategoryRepository categoryRepository, ArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category requireCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Category create(String name) {
        return categoryRepository.save(new Category(clean(name)));
    }

    @Transactional
    public void update(Long id, String name) {
        requireCategory(id).rename(clean(name));
    }

    @Transactional
    public void delete(Long id) {
        if (articleRepository.countByCategory_Id(id) > 0) {
            throw new IllegalArgumentException("已有文章使用该分类，无法删除");
        }
        categoryRepository.deleteById(id);
    }

    private String clean(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        return name.trim();
    }
}
