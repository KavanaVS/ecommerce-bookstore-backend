package com.bookstore.service;

import com.bookstore.dto.CategoryRequest;
import com.bookstore.dto.CategoryResponse;
import com.bookstore.dto.mapper.CategoryMapper;
import com.bookstore.exception.CategoryNotFoundException;
import com.bookstore.exception.DuplicateCategoryNameException;
import com.bookstore.model.Category;
import com.bookstore.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryRequest categoryRequest;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryRequest = new CategoryRequest();
        categoryRequest.setName("Fiction");
        categoryRequest.setDescription("Fictional works");

        category = Category.builder()
                .id(1L)
                .name("Fiction")
                .description("Fictional works")
                .build();
    }

    @Test
    void list_returnsAllCategoriesMapped() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        CategoryResponse expectedResponse = CategoryResponse.builder()
                .id(1L)
                .name("Fiction")
                .description("Fictional works")
                .build();
        when(categoryMapper.toCategoryResponse(category)).thenReturn(expectedResponse);

        List<CategoryResponse> result = categoryService.list();

        assertThat(result).containsExactly(expectedResponse);
    }

    @Test
    void create_whenNameExists_throwsDuplicateCategoryNameException() {
        when(categoryRepository.existsByName(categoryRequest.getName())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(categoryRequest))
                .isInstanceOf(DuplicateCategoryNameException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_whenNameIsNew_savesCategoryAndReturnsResponse() {
        when(categoryRepository.existsByName(categoryRequest.getName())).thenReturn(false);
        when(categoryMapper.toCategory(categoryRequest)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse expectedResponse = CategoryResponse.builder()
                .id(1L)
                .name("Fiction")
                .description("Fictional works")
                .build();
        when(categoryMapper.toCategoryResponse(category)).thenReturn(expectedResponse);

        CategoryResponse result = categoryService.create(categoryRequest);

        assertThat(result).isEqualTo(expectedResponse);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue()).isEqualTo(category);
    }

    @Test
    void getById_whenMissing_throwsCategoryNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void getById_whenFound_returnsCategoryResponse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse expectedResponse = CategoryResponse.builder()
                .id(1L)
                .name("Fiction")
                .description("Fictional works")
                .build();
        when(categoryMapper.toCategoryResponse(category)).thenReturn(expectedResponse);

        CategoryResponse result = categoryService.getById(1L);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void update_whenNewNameAlreadyUsedByAnotherCategory_throwsDuplicateCategoryNameException() {
        CategoryRequest renameRequest = new CategoryRequest();
        renameRequest.setName("Non-Fiction");
        renameRequest.setDescription("Non-fictional works");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Non-Fiction")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, renameRequest))
                .isInstanceOf(DuplicateCategoryNameException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_whenSuccessful_updatesCategoryAndReturnsResponse() {
        CategoryRequest updateRequest = new CategoryRequest();
        updateRequest.setName("Fiction");
        updateRequest.setDescription("Updated description");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse expectedResponse = CategoryResponse.builder()
                .id(1L)
                .name("Fiction")
                .description("Updated description")
                .build();
        when(categoryMapper.toCategoryResponse(category)).thenReturn(expectedResponse);

        CategoryResponse result = categoryService.update(1L, updateRequest);

        assertThat(result).isEqualTo(expectedResponse);
        verify(categoryMapper).updateCategoryFromRequest(updateRequest, category);
        verify(categoryRepository, never()).existsByName(any());
        verify(categoryRepository).save(category);
    }

    @Test
    void delete_whenMissing_throwsCategoryNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void delete_whenFound_deletesCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }
}
