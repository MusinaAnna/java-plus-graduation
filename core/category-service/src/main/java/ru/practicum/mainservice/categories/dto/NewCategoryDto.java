package ru.practicum.mainservice.categories.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания новой категории событий.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewCategoryDto {

    /**
     * Название новой категории.
     * Обязательное поле, не может быть пустым.
     * Длина от 1 до 50 символов.
     */
    @NotBlank(message = "Название категории не может быть пустым")
    @Size(min = 1, max = 50, message = "Название категории должно быть от 1 до 50 символов")
    private String name;
}