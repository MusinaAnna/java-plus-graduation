package ru.practicum.mainservice.compilations.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO для обновления существующей подборки событий.
 * Все поля опциональны — будут обновлены только переданные.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompilationRequest {

    /**
     * Новый список идентификаторов событий в подборке.
     * Если передан — полностью заменяет текущий список.
     */
    private Set<Long> events;

    /**
     * Новый флаг закрепления подборки на главной странице.
     */
    private Boolean pinned;

    /**
     * Новое название подборки.
     * Длина от 1 до 50 символов.
     */
    @Size(min = 1, max = 50, message = "Название подборки должно быть от 1 до 50 символов")
    private String title;
}