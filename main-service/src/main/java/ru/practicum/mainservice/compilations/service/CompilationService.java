package ru.practicum.mainservice.compilations.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.mainservice.compilations.dto.NewCompilationDto;
import ru.practicum.mainservice.compilations.dto.UpdateCompilationRequest;

import java.util.List;

/**
 * Сервис для управления подборками событий.
 * Предоставляет методы для создания, обновления, удаления и получения подборок.
 */
public interface CompilationService {

    /**
     * Создание новой подборки событий.
     *
     * @param dto данные новой подборки
     * @return созданная подборка
     */
    CompilationDto createCompilation(NewCompilationDto dto);

    /**
     * Обновление существующей подборки.
     *
     * @param compId идентификатор подборки
     * @param dto    данные для обновления
     * @return обновленная подборка
     */
    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto);

    /**
     * Удаление подборки по идентификатору.
     *
     * @param compId идентификатор подборки
     */
    void deleteCompilation(Long compId);

    /**
     * Получение списка подборок с фильтрацией по закреплению и пагинацией.
     *
     * @param pinned   фильтр по закрепленным/незакрепленным подборкам (опционально)
     * @param pageable параметры пагинации
     * @return список подборок
     */
    List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable);

    /**
     * Получение подборки по идентификатору.
     *
     * @param compId идентификатор подборки
     * @return подборка
     */
    CompilationDto getCompilation(Long compId);
}