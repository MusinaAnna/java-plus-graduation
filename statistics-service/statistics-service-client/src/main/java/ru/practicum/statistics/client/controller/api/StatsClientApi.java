package ru.practicum.statistics.client.controller.api;

import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.util.List;

/**
 * API клиента для взаимодействия с сервисом статистики.
 * Определяет методы для отправки хитов и получения статистики.
 */
public interface StatsClientApi {

    /**
     * Отправка хита в сервис статистики.
     *
     * @param hit данные хита (приложение, URI, IP, временная метка)
     */
    void sendHit(EndpointHit hit);

    /**
     * Получение статистики просмотров по заданным параметрам.
     *
     * @param start  начальная дата и время периода
     * @param end    конечная дата и время периода
     * @param uris   список URI для фильтрации (опционально)
     * @param unique флаг подсчета уникальных IP-адресов
     * @return список статистики просмотров
     */
    List<ViewStats> getStats(String start, String end, List<String> uris, Boolean unique);
}