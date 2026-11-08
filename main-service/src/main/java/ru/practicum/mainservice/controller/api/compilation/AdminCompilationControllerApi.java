package ru.practicum.mainservice.controller.api.compilation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.mainservice.compilations.dto.NewCompilationDto;
import ru.practicum.mainservice.compilations.dto.UpdateCompilationRequest;

/**
 * API для административного управления подборками событий.
 * Предоставляет методы для создания, обновления и удаления подборок.
 */
public interface AdminCompilationControllerApi {

    /**
     * Создание новой подборки событий.
     *
     * @param dto данные новой подборки
     * @return созданная подборка
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CompilationDto saveCompilation(@Valid @RequestBody NewCompilationDto dto);

    /**
     * Обновление существующей подборки.
     *
     * @param compId идентификатор подборки
     * @param dto    данные для обновления
     * @return обновленная подборка
     */
    @PatchMapping("/{compId}")
    CompilationDto updateCompilation(@PathVariable Long compId,
                                     @Valid @RequestBody(required = false) UpdateCompilationRequest dto);

    /**
     * Удаление подборки по идентификатору.
     *
     * @param compId идентификатор подборки
     */
    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCompilation(@PathVariable Long compId);
}