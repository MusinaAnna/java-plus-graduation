package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для отображения статистики просмотров.
 * Содержит информацию о сервисе, URI и количестве просмотров.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ViewStats {

    /**
     * Название сервиса.
     */
    private String app;

    /**
     * URI запрошенного эндпоинта.
     */
    private String uri;

    /**
     * Количество просмотров (хитов) для данного URI.
     */
    private Long hits;
}