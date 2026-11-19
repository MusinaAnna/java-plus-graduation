package ru.practicum.mainservice.comments.service;

import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ForbiddenException;
import ru.practicum.common.exception.NotFoundException;

import java.util.List;

/**
 * Сервисный интерфейс для управления комментариями.
 * <p>
 * Определяет контракт бизнес-логики для работы с комментариями к событиям.
 * Реализует основные операции: создание, обновление, удаление и получение комментариев
 * с разграничением прав доступа для пользователей и администраторов.
 *
 * <p>Основные сценарии использования:
 * <ul>
 *   <li>Пользователи создают, обновляют и удаляют (soft delete) свои комментарии</li>
 *   <li>Публичные запросы на получение комментариев к событиям</li>
 *   <li>Администраторы управляют комментариями (просмотр всех, hard delete)</li>
 * </ul>
 *
 * @see ru.practicum.mainservice.comments.service.impl.CommentServiceImpl
 * @see ru.practicum.mainservice.comments.repository.CommentRepository
 */
public interface CommentService {

    /**
     * Создает новый комментарий к событию.
     * <p>
     * Процесс создания:
     * <ol>
     *   <li>Проверяет существование пользователя и события</li>
     *   <li>Проверяет, что событие опубликовано (статус PUBLISHED)</li>
     *   <li>Опционально: проверяет участие пользователя в событии</li>
     *   <li>Создает комментарий с текущей датой и временем</li>
     * </ol>
     *
     * @param userId  идентификатор автора комментария
     * @param eventId идентификатор события
     * @param dto     данные нового комментария (текст)
     * @return созданный комментарий с заполненными полями (id, createdAt, автор)
     * @throws NotFoundException если пользователь или событие не найдены
     * @throws ConflictException если событие не опубликовано (статус не PUBLISHED)
     * @throws ConflictException если пользователь не участвовал в событии (опционально)
     */
    CommentDto createComment(Long userId, Long eventId, NewCommentDto dto);

    /**
     * Обновляет существующий комментарий.
     * <p>
     * Процесс обновления:
     * <ol>
     *   <li>Проверяет существование комментария</li>
     *   <li>Проверяет, что пользователь является автором комментария</li>
     *   <li>Проверяет, что комментарий не был удален (soft delete)</li>
     *   <li>Обновляет текст и дату последнего изменения</li>
     * </ol>
     *
     * @param userId    идентификатор пользователя (для проверки прав)
     * @param commentId идентификатор обновляемого комментария
     * @param dto       новые данные комментария (текст)
     * @return обновленный комментарий с обновленной датой updatedAt
     * @throws NotFoundException  если комментарий не найден
     * @throws ForbiddenException если пользователь не является автором комментария
     * @throws ConflictException  если комментарий был удален (isDeleted = true)
     */
    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto);

    /**
     * Удаляет комментарий пользователем (soft delete).
     * <p>
     * При мягком удалении:
     * <ul>
     *   <li>Комментарий становится невидимым для публичных запросов</li>
     *   <li>Остается в базе данных для администратора</li>
     *   <li>Может быть восстановлен администратором при необходимости</li>
     * </ul>
     *
     * <p>Процесс удаления:
     * <ol>
     *   <li>Проверяет существование комментария</li>
     *   <li>Проверяет, что пользователь является автором</li>
     *   <li>Устанавливает флаг isDeleted = true</li>
     * </ol>
     *
     * @param userId    идентификатор пользователя (для проверки прав)
     * @param commentId идентификатор удаляемого комментария
     * @throws NotFoundException  если комментарий не найден
     * @throws ForbiddenException если пользователь не является автором комментария
     */
    void deleteCommentByUser(Long userId, Long commentId);

    /**
     * Получает список активных комментариев к событию с пагинацией и сортировкой.
     * <p>
     * Доступно всем пользователям без авторизации.
     * Возвращает только комментарии с isDeleted = false.
     *
     * <p>Сортировка возможна по любому полю комментария (по умолчанию - createdAt).
     *
     * @param eventId идентификатор события
     * @param from    начальная позиция для пагинации (0-based)
     * @param size    количество записей на странице
     * @param sort    поле для сортировки (например, "createdAt", "text", "author.id")
     * @return список комментариев события (только активные)
     * @throws NotFoundException если событие не найдено или не опубликовано
     */
    List<CommentDto> getEventComments(Long eventId, int from, int size, String sort);

    /**
     * Получает конкретный комментарий по его идентификатору.
     * <p>
     * Доступно всем пользователям без авторизации.
     * Возвращает комментарий только если он активен (isDeleted = false)
     * и принадлежит указанному событию.
     *
     * @param eventId   идентификатор события (для валидации)
     * @param commentId идентификатор комментария
     * @return комментарий
     * @throws NotFoundException если комментарий не найден, удален или не принадлежит событию
     */
    CommentDto getComment(Long eventId, Long commentId);

    /**
     * Получает все комментарии пользователя (включая удаленные).
     * <p>
     * Доступно только администраторам.
     * Используется для модерации и аудита.
     *
     * <p>Возвращает все комментарии пользователя, включая помеченные как isDeleted = true.
     *
     * @param userId идентификатор пользователя
     * @param from   начальная позиция для пагинации (0-based)
     * @param size   количество записей на странице
     * @param sort   поле для сортировки
     * @return список всех комментариев пользователя
     * @throws NotFoundException если пользователь не найден
     */
    List<CommentDto> getUserCommentsByAdmin(Long userId, int from, int size, String sort);

    /**
     * Жестко удаляет комментарий из базы данных (hard delete).
     * <p>
     * Доступно только администраторам.
     * Комментарий полностью удаляется из БД и не может быть восстановлен.
     * Отличается от мягкого удаления (soft delete) физическим удалением записи.
     *
     * @param commentId идентификатор удаляемого комментария
     * @throws NotFoundException если комментарий не найден
     */
    void deleteCommentByAdmin(Long commentId);

    /**
     * Получает все комментарии события (включая удаленные).
     * <p>
     * Доступно только администраторам.
     * Используется для модерации контента и анализа активности.
     *
     * <p>Возвращает все комментарии события, включая помеченные как isDeleted = true.
     *
     * @param eventId идентификатор события
     * @param from    начальная позиция для пагинации (0-based)
     * @param size    количество записей на странице
     * @param sort    поле для сортировки
     * @return список всех комментариев события
     * @throws NotFoundException если событие не найдено
     */
    List<CommentDto> getEventCommentsByAdmin(Long eventId, int from, int size, String sort);
}