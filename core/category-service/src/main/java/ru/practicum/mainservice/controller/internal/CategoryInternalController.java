package ru.practicum.mainservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.mainservice.categories.service.CategoryService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/categories")
public class CategoryInternalController {
    private final CategoryService categoryService;

    @GetMapping("/{categoryId}")
    public CategoryDto getCategory(@PathVariable("categoryId") Long categoryId) {
        return categoryService.getCategory(categoryId);
    }

    @GetMapping
    public List<CategoryDto> getCategories(@RequestParam("ids") List<Long> ids) {
        return categoryService.getCategoriesByIds(ids);
    }
}
