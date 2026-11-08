package ru.practicum.statistics.server.service;

import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.server.model.HitEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для работы со статистикой.
 * Предоставляет методы для сохранения хитов и получения статистики просмотров.
 */
public interface StatsService {

    /**
     * Сохранение нового хита в базе данных.
     *
     * @param endpointHit данные хита
     * @return сохраненная сущность HitEntity
     */
    HitEntity save(EndpointHit endpointHit);

    /**
     * Получение статистики просмотров за указанный период с фильтрацией по URI.
     *
     * @param start  начальная дата и время периода
     * @param end    конечная дата и время периода
     * @param uris   список URI для фильтрации (опционально)
     * @param unique флаг подсчета уникальных IP-адресов
     * @return список статистики просмотров
     */
    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}