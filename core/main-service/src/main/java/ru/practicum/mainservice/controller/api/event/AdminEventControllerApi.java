package ru.practicum.mainservice.controller.api.event;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

/**
 * Административное API для управления событиями.
 * Предоставляет методы для поиска, фильтрации и модерации событий.
 */
public interface AdminEventControllerApi {

    /**
     * Получение списка событий с фильтрацией по административным параметрам.
     *
     * @param users      список идентификаторов пользователей-инициаторов (опционально)
     * @param states     список состояний событий (опционально)
     * @param categories список идентификаторов категорий (опционально)
     * @param rangeStart дата и время начала диапазона (опционально)
     * @param rangeEnd   дата и время окончания диапазона (опционально)
     * @param from       начальная позиция для пагинации
     * @param size       размер страницы
     * @return список событий с полной информацией
     */
    @GetMapping
    Collection<EventFullDto> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size);

    /**
     * Обновление события администратором (включая публикацию и отклонение).
     *
     * @param eventId идентификатор события
     * @param request данные для обновления события
     * @return обновленное событие с полной информацией
     */
    @PatchMapping("/{eventId}")
    EventFullDto updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest request);
}