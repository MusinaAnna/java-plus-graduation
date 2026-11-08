package ru.practicum.mainservice.controller.api.compilation;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.common.dto.CompilationDto;

import java.util.List;

/**
 * Публичное API для получения информации о подборках событий.
 * Предоставляет методы для получения списка подборок и подборки по идентификатору.
 */
public interface PublicCompilationControllerApi {

    /**
     * Получение списка подборок событий с фильтрацией по закреплению и пагинацией.
     *
     * @param pinned фильтр по закрепленным/незакрепленным подборкам (опционально)
     * @param from   начальная позиция для пагинации
     * @param size   размер страницы
     * @return список подборок событий
     */
    @GetMapping
    List<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size);

    /**
     * Получение подборки событий по идентификатору.
     *
     * @param compId идентификатор подборки
     * @return подборка событий
     */
    @GetMapping("/{compId}")
    CompilationDto getCompilation(@PathVariable Long compId);
}