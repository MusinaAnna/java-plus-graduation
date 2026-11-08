package ru.practicum.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO для передачи информации о подборке событий.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompilationDto {

    /**
     * Список событий, входящих в подборку.
     */
    private Set<EventShortDto> events;

    /**
     * Идентификатор подборки.
     */
    private Long id;

    /**
     * Флаг закрепления подборки на главной странице.
     */
    private Boolean pinned;

    /**
     * Название подборки.
     */
    private String title;
}