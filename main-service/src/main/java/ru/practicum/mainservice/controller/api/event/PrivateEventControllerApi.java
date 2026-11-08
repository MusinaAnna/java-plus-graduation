package ru.practicum.mainservice.controller.api.event;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;

import java.util.List;

/**
 * API для приватного управления событиями текущего пользователя.
 * Предоставляет методы для создания, получения и обновления событий пользователя.
 */
public interface PrivateEventControllerApi {

    /**
     * Получение списка событий, созданных текущим пользователем.
     *
     * @param userId идентификатор пользователя
     * @param from   начальная позиция для пагинации
     * @param size   размер страницы
     * @return список кратких DTO событий
     */
    @GetMapping
    List<EventShortDto> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size);

    /**
     * Создание нового события от имени текущего пользователя.
     *
     * @param userId идентификатор пользователя
     * @param dto    данные нового события
     * @return полная информация о созданном событии
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EventFullDto createEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto dto);

    /**
     * Получение полной информации о событии текущего пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return полная информация о событии
     */
    @GetMapping("/{eventId}")
    EventFullDto getEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId);

    /**
     * Обновление события текущего пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @param request данные для обновления события
     * @return обновленная информация о событии
     */
    @PatchMapping("/{eventId}")
    EventFullDto updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest request);
}