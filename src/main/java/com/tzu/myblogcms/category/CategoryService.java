package com.tzu.myblogcms.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
        categoryRepository.deleteById(id);
    }

    private String clean(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        return name.trim();
    }
}
