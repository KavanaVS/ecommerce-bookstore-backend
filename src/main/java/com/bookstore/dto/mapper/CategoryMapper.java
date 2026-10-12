package com.bookstore.dto.mapper;

import com.bookstore.dto.CategoryRequest;
import com.bookstore.dto.CategoryResponse;
import com.bookstore.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toCategoryResponse(Category category);

    Category toCategory(CategoryRequest request);

    void updateCategoryFromRequest(CategoryRequest request, @MappingTarget Category category);
}
