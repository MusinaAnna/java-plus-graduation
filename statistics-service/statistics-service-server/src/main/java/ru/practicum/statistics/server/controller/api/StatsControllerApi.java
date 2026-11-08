package ru.practicum.statistics.server.controller.api;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

/**
 * API для работы со статистикой.
 * Предоставляет методы для получения статистики просмотров и записи хитов.
 */
public interface StatsControllerApi {

    /**
     * Получение статистики просмотров за указанный период с фильтрацией по URI.
     *
     * @param start  начальная дата и время периода (обязательный)
     * @param end    конечная дата и время периода (обязательный)
     * @param uris   список URI для фильтрации (опционально)
     * @param unique флаг подсчета уникальных IP-адресов
     * @return список статистики просмотров
     */
    @GetMapping("/stats")
    Collection<ViewStats> getAll(
            @RequestParam(required = true) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime start,
            @RequestParam(required = true) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique);

    /**
     * Запись нового хита (запроса) в статистику.
     *
     * @param endpointHit данные хита
     */
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    void post(@RequestBody EndpointHit endpointHit);
}