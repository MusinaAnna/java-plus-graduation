package ru.practicum.mainservice.controller.api.event;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.mainservice.events.dto.EventFullDto;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

/**
 * Публичное API для получения информации о событиях без авторизации.
 * Все эндпоинты возвращают только опубликованные события.
 *
 * @see EventFullDto
 * @see EventShortDto
 */
public interface PublicEventControllerApi {

    /**
     * Получение списка опубликованных событий с фильтрацией и пагинацией.
     *
     * @param text          текст для поиска в аннотации и описании события (опционально)
     * @param categories    список идентификаторов категорий для фильтрации (опционально)
     * @param paid          фильтр по платности/бесплатности события (опционально)
     * @param rangeStart    дата и время начала диапазона (опционально)
     * @param rangeEnd      дата и время окончания диапазона (опционально)
     * @param onlyAvailable фильтр для показа только событий с неисчерпанным лимитом запросов
     * @param sort          вариант сортировки: EVENT_DATE или VIEWS (опционально)
     * @param from          начальная позиция для пагинации
     * @param size          размер страницы
     * @return список кратких DTO событий с просмотрами и заявками
     */
    @GetMapping
    List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size);

    /**
     * Получение полной информации об опубликованном событии по идентификатору.
     *
     * @param id идентификатор события
     * @return полная информация о событии
     */
    @GetMapping("/{id}")
    EventFullDto getEvent(@PathVariable Long id);
}