package ru.practicum.mainservice.controller.api.comment;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.CommentDto;

import java.util.List;

/**
 * API интерфейс для административных эндпоинтов работы с комментариями.
 * <p>
 * Определяет контракт для управления комментариями администратором.
 * Доступен только для пользователей с ролью ADMIN.
 * Предоставляет расширенные возможности:
 * <ul>
 *   <li>Просмотр всех комментариев (включая удаленные)</li>
 *   <li>Жесткое удаление комментариев (hard delete)</li>
 *   <li>Просмотр комментариев любого пользователя</li>
 * </ul>
 *
 * @see ru.practicum.mainservice.controller.comment.AdminCommentController
 */
@Validated
public interface AdminCommentControllerApi {

    /**
     * Получить все комментарии указанного пользователя (включая удаленные).
     * <p>
     * Администратор может просматривать все комментарии любого пользователя.
     *
     * @param userId идентификатор пользователя (должен быть положительным)
     * @param from   начальная позиция для пагинации (по умолчанию 0)
     * @param size   количество записей на странице (по умолчанию 10)
     * @param sort   поле для сортировки (по умолчанию "createdAt")
     * @return список всех комментариев пользователя
     */
    @GetMapping("/users/{userId}")
    List<CommentDto> getUserComments(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size,
            @RequestParam(defaultValue = "createdAt") String sort
    );

    /**
     * Получить все комментарии указанного события (включая удаленные).
     *
     * @param eventId идентификатор события (должен быть положительным)
     * @param from    начальная позиция для пагинации (по умолчанию 0)
     * @param size    количество записей на странице (по умолчанию 10)
     * @param sort    поле для сортировки (по умолчанию "createdAt")
     * @return список всех комментариев события
     */
    @GetMapping("/events/{eventId}")
    List<CommentDto> getEventComments(
            @PathVariable @Positive Long eventId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size,
            @RequestParam(defaultValue = "createdAt") String sort
    );

    /**
     * Жестко удалить комментарий (hard delete).
     * <p>
     * Полностью удаляет комментарий из базы данных.
     * Отличие от мягкого удаления (soft delete):
     * <ul>
     *   <li>Комментарий физически удаляется из БД</li>
     *   <li>Невозможно восстановить удаленный комментарий</li>
     *   <li>Используется для полной очистки данных</li>
     * </ul>
     *
     * @param commentId идентификатор комментария (должен быть положительным)
     */
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteComment(
            @PathVariable @Positive Long commentId
    );
}
