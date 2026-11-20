package ru.practicum.mainservice.categories.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.mainservice.categories.dto.NewCategoryDto;

import java.util.List;


public interface CategoryService {
    CategoryDto createCategory(NewCategoryDto dto);

    CategoryDto updateCategory(Long catId, CategoryDto dto);

    void deleteCategory(Long catId);

    List<CategoryDto> getCategories(Pageable pageable);

    CategoryDto getCategory(Long catId);

    List<CategoryDto> getCategoriesByIds(List<Long> ids);
}
