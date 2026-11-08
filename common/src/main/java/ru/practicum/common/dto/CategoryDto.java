package ru.practicum.common.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для передачи информации о категории событий.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {

    /**
     * Идентификатор категории.
     */
    private Long id;

    /**
     * Название категории. Максимальная длина 50 символов.
     */
    @Size(max = 50, message = "Название категории не должно превышать 50 символов")
    private String name;
}