package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

/**
 * DTO для записи запроса (хита) к сервису статистики.
 * Содержит информацию о сервисе, URI, IP-адресе и временной метке.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EndpointHit {

    /**
     * Название сервиса, отправившего запрос.
     * Не может быть пустым.
     */
    @NotBlank(message = "Название сервиса не должно быть пустым")
    private String app;

    /**
     * URI запрошенного эндпоинта.
     * Не может быть пустым.
     */
    @NotBlank(message = "URI не должен быть пустым")
    private String uri;

    /**
     * IP-адрес клиента, отправившего запрос.
     * Должен быть валидным IPv4-адресом.
     * Не может быть пустым.
     */
    @NotBlank(message = "IP-адрес не должен быть пустым")
    @Pattern(
            regexp = "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$",
            message = "Неверный формат IP-адреса"
    )
    private String ip;

    /**
     * Временная метка запроса.
     * Формат: yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime timestamp;
}