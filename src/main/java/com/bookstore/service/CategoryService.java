package com.bookstore.service;

import com.bookstore.dto.CategoryRequest;
import com.bookstore.dto.CategoryResponse;
import com.bookstore.dto.mapper.CategoryMapper;
import com.bookstore.exception.CategoryNotFoundException;
import com.bookstore.exception.DuplicateCategoryNameException;
import com.bookstore.model.Category;
import com.bookstore.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    public CategoryResponse getById(Long id) {
        Category category = findCategoryOrThrow(id);
        return categoryMapper.toCategoryResponse(category);
    }

    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateCategoryNameException("A category with this name already exists");
        }

        Category category = categoryMapper.toCategory(request);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(saved);
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);

        boolean nameChanged = !category.getName().equals(request.getName());
        if (nameChanged && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateCategoryNameException("A category with this name already exists");
        }

        categoryMapper.updateCategoryFromRequest(request, category);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(saved);
    }

    public void delete(Long id) {
        Category category = findCategoryOrThrow(id);
        categoryRepository.delete(category);
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    }
}
