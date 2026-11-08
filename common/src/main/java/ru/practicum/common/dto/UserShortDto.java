package ru.practicum.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для краткой информации о пользователе.
 * Используется в ответах API для отображения основных данных без email.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserShortDto {

    /**
     * Идентификатор пользователя.
     */
    private Long id;

    /**
     * Имя пользователя.
     */
    private String name;
}