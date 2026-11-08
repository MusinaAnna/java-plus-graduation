package ru.practicum.mainservice.controller.api.comment;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.common.dto.CommentDto;

import java.util.List;

/**
 * API интерфейс для публичных эндпоинтов работы с комментариями.
 * <p>
 * Определяет контракт для получения комментариев к событиям.
 * Доступен без авторизации.
 *
 * @see ru.practicum.mainservice.controller.comment.PublicCommentController
 */
@Validated
public interface PublicCommentControllerApi {

    /**
     * Получить все комментарии события с пагинацией и сортировкой.
     * <p>
     * Возвращает только активные комментарии (isDeleted = false).
     *
     * @param eventId идентификатор события (должен быть положительным)
     * @param from    начальная позиция для пагинации (по умолчанию 0)
     * @param size    количество записей на странице (по умолчанию 10)
     * @param sort    поле для сортировки (по умолчанию "createdAt")
     * @return список комментариев к событию
     */
    @GetMapping
    List<CommentDto> getComments(
            @PathVariable @Positive Long eventId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size,
            @RequestParam(defaultValue = "createdAt") String sort
    );

    /**
     * Получить конкретный комментарий по его идентификатору.
     * <p>
     * Возвращает комментарий только если он активен (isDeleted = false).
     * Если комментарий удален (soft delete) - возвращает 404 Not Found.
     *
     * @param eventId   идентификатор события (должен быть положительным)
     * @param commentId идентификатор комментария (должен быть положительным)
     * @return комментарий
     */
    @GetMapping("/{commentId}")
    CommentDto getComment(
            @PathVariable @Positive Long eventId,
            @PathVariable @Positive Long commentId
    );
}
