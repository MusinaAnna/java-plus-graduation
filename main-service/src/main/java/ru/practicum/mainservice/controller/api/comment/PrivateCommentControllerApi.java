package ru.practicum.mainservice.controller.api.comment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;

/**
 * API интерфейс для приватных эндпоинтов работы с комментариями.
 * <p>
 * Определяет контракт для управления комментариями авторизованными пользователями.
 * Доступен только для аутентифицированных пользователей.
 *
 * @see ru.practicum.mainservice.controller.comment.PrivateCommentController
 */
@Validated
public interface PrivateCommentControllerApi {

    /**
     * Создать новый комментарий к событию.
     * <p>
     * Пользователь может оставить комментарий только к опубликованному событию.
     * Опционально: только если пользователь был участником события.
     *
     * @param userId  идентификатор пользователя (из пути)
     * @param eventId идентификатор события (из пути)
     * @param request DTO с текстом нового комментария
     * @return созданный комментарий с заполненными полями (id, createdAt, etc.)
     */
    @PostMapping("/events/{eventId}")
    CommentDto createComment(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody NewCommentDto request
    );

    /**
     * Обновить существующий комментарий.
     * <p>
     * Пользователь может обновлять только свои комментарии.
     * Нельзя обновить удаленный комментарий (soft delete).
     *
     * @param userId    идентификатор пользователя (из пути)
     * @param commentId идентификатор комментария (из пути)
     * @param request   DTO с обновленным текстом комментария
     * @return обновленный комментарий с обновленной датой updatedAt
     */
    @PatchMapping("/{commentId}")
    CommentDto updateComment(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long commentId,
            @Valid @RequestBody UpdateCommentDto request
    );

    /**
     * Удалить комментарий (soft delete).
     * <p>
     * Пользователь может удалять только свои комментарии.
     * Комментарий становится невидимым для других пользователей (isDeleted = true),
     * но остается в базе данных для администратора.
     *
     * @param userId    идентификатор пользователя (из пути)
     * @param commentId идентификатор комментария (из пути)
     */
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteComment(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long commentId
    );
}
