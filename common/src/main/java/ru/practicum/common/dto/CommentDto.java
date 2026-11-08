package ru.practicum.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) для передачи данных о комментарии между слоями приложения.
 * <p>
 * Используется для:
 * <ul>
 *   <li>Отображения комментариев в ответах API</li>
 *   <li>Передачи данных между сервисным и контроллерным слоями</li>
 *   <li>Формирования JSON-ответов для клиентов</li>
 * </ul>
 *
 * <p>Содержит полную информацию о комментарии, включая данные автора,
 * время создания и статус удаления.
 *
 * @see NewCommentDto
 * @see UpdateCommentDto
 * @see ru.practicum.mainservice.comments.service.CommentService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    /**
     * Уникальный идентификатор комментария.
     * Генерируется автоматически при создании.
     */
    private Long id;

    /**
     * Текст комментария.
     * Содержит содержание комментария к событию.
     * Максимальная длина: 2000 символов.
     */
    private String text;

    /**
     * Идентификатор события, к которому относится комментарий.
     * Связывает комментарий с конкретным событием.
     */
    private Long eventId;

    /**
     * Краткая информация об авторе комментария.
     * Содержит только ID и имя пользователя.
     *
     * @see UserShortDto
     */
    private UserShortDto author;

    /**
     * Дата и время создания комментария.
     * Устанавливается автоматически при сохранении.
     * Не изменяется при обновлении.
     */
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления комментария.
     * Обновляется при каждом изменении текста.
     * Может быть null, если комментарий не обновлялся.
     */
    private LocalDateTime updatedAt;

    /**
     * Флаг мягкого удаления комментария.
     * <ul>
     *   <li>{@code true} - комментарий удален пользователем (не отображается публично)</li>
     *   <li>{@code false} - комментарий активен и виден всем</li>
     * </ul>
     * Используется для реализации soft-delete без физического удаления из БД.
     */
    private Boolean isDeleted;
}