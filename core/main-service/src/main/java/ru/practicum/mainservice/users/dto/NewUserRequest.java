package ru.practicum.mainservice.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания нового пользователя.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewUserRequest {

    /**
     * Электронная почта пользователя.
     * Обязательное поле, должно быть валидным email-адресом.
     * Длина от 6 до 254 символов.
     */
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Неверный формат email")
    @Size(min = 6, max = 254, message = "Email должен быть от 6 до 254 символов")
    private String email;

    /**
     * Имя пользователя.
     * Обязательное поле, не может быть пустым.
     * Длина от 2 до 250 символов.
     */
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 250, message = "Имя должно быть от 2 до 250 символов")
    private String name;
}