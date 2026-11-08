package ru.practicum.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания нового комментария.
 * <p>
 * Используется при создании комментария к событию авторизованным пользователем.
 * Содержит только текст комментария, так как остальные данные (автор, событие)
 * определяются из контекста запроса (путь и аутентификация).
 *
 * <p>Применяется в эндпоинте:
 * <pre>
 * POST /users/{userId}/comments/events/{eventId}
 * </pre>
 *
 * @see CommentDto
 * @see UpdateCommentDto
 * @see ru.practicum.mainservice.controller.PrivateCommentController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {

    /**
     * Текст нового комментария.
     * <p>
     * Обязательное поле для заполнения.
     * <ul>
     *   <li>Не может быть {@code null} или пустым</li>
     *   <li>Минимальная длина: 1 символ</li>
     *   <li>Максимальная длина: 2000 символов</li>
     * </ul>
     *
     * @see NotBlank - проверяет, что поле не null, не пустое и не содержит только пробелы
     * @see Size - проверяет длину строки
     */
    @NotBlank(message = "Текст комментария не может быть пустым")
    @Size(min = 1, max = 2000, message = "Длина комментария должна быть от 1 до 2000 символов")
    private String text;
}