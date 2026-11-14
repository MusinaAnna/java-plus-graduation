package ru.practicum.mainservice.events.service;

import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.entity.Event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для управления событиями.
 * Предоставляет методы для административного, приватного и публичного доступа к событиям.
 * Включает фильтрацию, создание, обновление, публикацию и получение событий.
 */
public interface EventService {

    // Admin

    /**
     * Получение списка событий с фильтрацией для администратора.
     * Возвращает полную информацию о событиях, соответствующих переданным условиям.
     *
     * @param users      список идентификаторов пользователей (опционально)
     * @param states     список состояний событий (опционально)
     * @param categories список идентификаторов категорий (опционально)
     * @param rangeStart дата и время начала диапазона (опционально)
     * @param rangeEnd   дата и время окончания диапазона (опционально)
     * @param from       начальная позиция для пагинации
     * @param size       размер страницы
     * @return список событий с полной информацией
     */
    List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states,
                                      List<Long> categories, LocalDateTime rangeStart,
                                      LocalDateTime rangeEnd, int from, int size);

    /**
     * Обновление события администратором (включая публикацию и отклонение).
     *
     * @param eventId идентификатор события
     * @param request данные для обновления
     * @return обновленное событие с полной информацией
     */
    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    // Private

    /**
     * Получение событий, созданных текущим пользователем.
     *
     * @param userId идентификатор пользователя
     * @param from   начальная позиция для пагинации
     * @param size   размер страницы
     * @return список событий с краткой информацией
     */
    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    /**
     * Создание нового события от имени пользователя.
     * Дата события должна быть минимум через 2 часа от текущего момента.
     *
     * @param userId идентификатор пользователя
     * @param dto    данные нового события
     * @return созданное событие с полной информацией (статус PENDING)
     */
    EventFullDto createEvent(Long userId, NewEventDto dto);

    /**
     * Получение полной информации о событии пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return полная информация о событии
     */
    EventFullDto getUserEvent(Long userId, Long eventId);

    /**
     * Обновление события пользователя.
     * Доступно только для событий в статусе PENDING или CANCELED.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @param request данные для обновления
     * @return обновленное событие с полной информацией
     */
    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    // Public

    /**
     * Получение опубликованных событий с возможностью фильтрации.
     * Поиск по тексту выполняется без учета регистра.
     * Информация о каждом событии включает просмотры и подтвержденные заявки.
     *
     * @param text          текст для поиска в аннотации и описании (опционально)
     * @param categories    список идентификаторов категорий (опционально)
     * @param paid          фильтр по платности (опционально)
     * @param rangeStart    дата и время начала диапазона (опционально)
     * @param rangeEnd      дата и время окончания диапазона (опционально)
     * @param onlyAvailable только события с неисчерпанным лимитом запросов
     * @param sort          вариант сортировки: EVENT_DATE или VIEWS
     * @param from          начальная позиция для пагинации
     * @param size          размер страницы
     * @return список событий с краткой информацией
     */
    List<EventShortDto> getPublicEvents(String text, List<Long> categories,
                                        Boolean paid, LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd, Boolean onlyAvailable,
                                        String sort, int from, int size);

    /**
     * Получение подробной информации об опубликованном событии.
     * Информация включает просмотры и подтвержденные запросы.
     *
     * @param eventId идентификатор события
     * @return полная информация о событии
     */
    EventFullDto getPublicEvent(Long eventId);

    /**
     * Получение JPA-сущности события по идентификатору для внутренних вызовов.
     *
     * @param eventId идентификатор события
     * @return сущность события
     */
    Event getEventEntity(Long eventId);

    /**
     * Проверка существования события по идентификатору.
     *
     * @param eventId идентификатор события
     * @return true - если событие существует, false - если нет
     */
    boolean existsById(Long eventId);

    /**
     * Получение списка сущностей событий по списку идентификаторов.
     *
     * @param eventIds список идентификаторов событий
     * @return список сущностей событий (только существующие)
     */
    List<Event> findAllByIds(List<Long> eventIds);
}