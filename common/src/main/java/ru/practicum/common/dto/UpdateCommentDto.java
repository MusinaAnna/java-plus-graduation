package ru.practicum.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для обновления существующего комментария.
 * <p>
 * Используется при редактировании комментария автором или администратором.
 * Содержит только текст комментария, так как идентификаторы комментария и автора
 * передаются через путь запроса.
 *
 * <p>Применяется в эндпоинтах:
 * <pre>
 * PATCH /users/{userId}/comments/{commentId}
 * </pre>
 *
 * <p>Отличие от {@link NewCommentDto}:
 * <ul>
 *   <li>Используется для обновления, а не создания</li>
 *   <li>Может применяться в админских эндпоинтах</li>
 *   <li>Требует наличия существующего комментария в БД</li>
 * </ul>
 *
 * @see CommentDto
 * @see NewCommentDto
 * @see ru.practicum.mainservice.controller.PrivateCommentController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentDto {

    /**
     * Обновленный текст комментария.
     * <p>
     * Обязательное поле для заполнения при обновлении.
     * <ul>
     *   <li>Не может быть {@code null} или пустым</li>
     *   <li>Минимальная длина: 1 символ</li>
     *   <li>Максимальная длина: 2000 символов</li>
     * </ul>
     *
     * <p>При обновлении полностью заменяет существующий текст комментария,
     * а не дополняет его.
     *
     * @see NotBlank - проверяет, что поле не null, не пустое и не содержит только пробелы
     * @see Size - проверяет длину строки
     */
    @NotBlank(message = "Текст комментария не может быть пустым")
    @Size(min = 1, max = 2000, message = "Длина комментария должна быть от 1 до 2000 символов")
    private String text;
}