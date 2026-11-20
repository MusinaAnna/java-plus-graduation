package ru.practicum.mainservice.controller.api.category;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.mainservice.categories.dto.NewCategoryDto;

/**
 * API для административного управления категориями событий.
 * Предоставляет методы для создания, обновления и удаления категорий.
 */
public interface AdminCategoryControllerApi {

    /**
     * Создание новой категории.
     *
     * @param dto данные новой категории
     * @return созданная категория
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryDto addCategory(@Valid @RequestBody NewCategoryDto dto);

    /**
     * Обновление существующей категории.
     *
     * @param catId идентификатор категории
     * @param dto   данные для обновления
     * @return обновленная категория
     */
    @PatchMapping("/{catId}")
    CategoryDto updateCategory(@PathVariable Long catId, @Valid @RequestBody CategoryDto dto);

    /**
     * Удаление категории по идентификатору.
     *
     * @param catId идентификатор категории
     */
    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable Long catId);
}