package ru.practicum.mainservice.controller.api.category;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.common.dto.CategoryDto;

import java.util.List;

/**
 * Публичное API для получения информации о категориях событий.
 * Предоставляет методы для получения списка категорий и категории по идентификатору.
 */
public interface PublicCategoryControllerApi {

    /**
     * Получение списка категорий с пагинацией.
     *
     * @param from начальная позиция для пагинации
     * @param size размер страницы
     * @return список категорий
     */
    @GetMapping
    List<CategoryDto> getCategories(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size);

    /**
     * Получение категории по идентификатору.
     *
     * @param catId идентификатор категории
     * @return категория
     */
    @GetMapping("/{catId}")
    CategoryDto getCategory(@PathVariable Long catId);
}