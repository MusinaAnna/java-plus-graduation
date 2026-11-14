package ru.practicum.mainservice.controller.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.mainservice.categories.dto.NewCategoryDto;
import ru.practicum.mainservice.categories.service.CategoryService;
import ru.practicum.mainservice.controller.api.category.AdminCategoryControllerApi;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class AdminCategoryController implements AdminCategoryControllerApi {

    private final CategoryService categoryService;

    @Override
    public CategoryDto addCategory(@Valid @RequestBody NewCategoryDto dto) {
        log.info("POST /admin/categories with request: {}", dto);
        return categoryService.createCategory(dto);
    }

    @Override
    public CategoryDto updateCategory(
            @PathVariable Long catId,
            @Valid @RequestBody CategoryDto dto) {
        log.info("PATCH /admin/categories/{} with request: {}", catId, dto);
        return categoryService.updateCategory(catId, dto);
    }

    @Override
    public void deleteCategory(@PathVariable Long catId) {
        log.info("DELETE /admin/categories/{}", catId);
        categoryService.deleteCategory(catId);
    }
}
