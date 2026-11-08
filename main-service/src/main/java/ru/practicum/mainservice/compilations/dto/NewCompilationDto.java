package ru.practicum.mainservice.compilations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO для создания новой подборки событий.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewCompilationDto {

    /**
     * Список идентификаторов событий, входящих в подборку.
     * Может быть пустым (подборка без событий).
     */
    private Set<Long> events;

    /**
     * Флаг закрепления подборки на главной странице.
     * По умолчанию false.
     */
    private Boolean pinned = false;

    /**
     * Название подборки.
     * Обязательное поле, не может быть пустым.
     * Длина от 1 до 50 символов.
     */
    @NotBlank(message = "Название подборки не может быть пустым")
    @Size(min = 1, max = 50, message = "Название подборки должно быть от 1 до 50 символов")
    private String title;
}