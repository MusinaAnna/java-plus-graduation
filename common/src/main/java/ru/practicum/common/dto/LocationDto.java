package ru.practicum.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для передачи информации о местоположении события.
 * Содержит широту и долготу в градусах.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto {

    /**
     * Широта в градусах.
     */
    private Float lat;

    /**
     * Долгота в градусах.
     */
    private Float lon;
}