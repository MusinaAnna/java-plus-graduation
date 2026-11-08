package ru.practicum.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для передачи информации об ошибке в ответе API.
 * Содержит детали ошибки, статус и временную метку.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {

    /**
     * Список ошибок или стектрейсов (опционально).
     */
    private List<String> errors;

    /**
     * Сообщение об ошибке.
     */
    private String message;

    /**
     * Краткое описание причины ошибки.
     */
    private String reason;

    /**
     * Код статуса HTTP-ответа (например, "BAD_REQUEST", "NOT_FOUND").
     */
    private String status;

    /**
     * Дата и время возникновения ошибки в формате "yyyy-MM-dd HH:mm:ss".
     */
    private String timestamp;
}