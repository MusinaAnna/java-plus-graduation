package ru.practicum.statistics.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Сущность для хранения информации о запросе (хите) в сервисе статистики.
 * Содержит данные о сервисе, URI, IP-адресе и временной метке запроса.
 */
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@ToString
public class HitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название сервиса, отправившего запрос.
     */
    @Column(nullable = false)
    private String app;

    /**
     * URI запрошенного эндпоинта.
     */
    @Column(nullable = false)
    private String uri;

    /**
     * IP-адрес клиента, отправившего запрос.
     */
    @Column(nullable = false)
    private String ip;

    /**
     * Временная метка запроса.
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;
}