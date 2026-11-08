package ru.practicum.mainservice.controller.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.mainservice.categories.service.CategoryService;
import ru.practicum.mainservice.controller.api.category.PublicCategoryControllerApi;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class PublicCategoryController implements PublicCategoryControllerApi {

    private final CategoryService categoryService;

    @Override
    public List<CategoryDto> getCategories(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /categories with from={}, size={}", from, size);
        return categoryService.getCategories(PageRequest.of(from / size, size));
    }

    @Override
    public CategoryDto getCategory(@PathVariable Long catId) {
        log.info("GET /categories/{}", catId);
        return categoryService.getCategory(catId);
    }
}
