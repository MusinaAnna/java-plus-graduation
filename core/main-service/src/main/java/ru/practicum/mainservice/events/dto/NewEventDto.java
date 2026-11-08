package ru.practicum.mainservice.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.common.config.JacksonConfig;
import ru.practicum.common.dto.LocationDto;

import java.time.LocalDateTime;

/**
 * DTO для создания нового события пользователем.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewEventDto {

    /**
     * Краткое описание события. От 20 до 2000 символов.
     */
    @NotBlank(message = "Аннотация не может быть пустой")
    @Size(min = 20, max = 2000, message = "Аннотация должна быть от 20 до 2000 символов")
    private String annotation;

    /**
     * Идентификатор категории события.
     */
    @NotNull(message = "Категория обязательна")
    private Long category;

    /**
     * Полное описание события. От 20 до 7000 символов.
     */
    @NotBlank(message = "Описание не может быть пустым")
    @Size(min = 20, max = 7000, message = "Описание должно быть от 20 до 7000 символов")
    private String description;

    /**
     * Дата и время проведения события.
     * Должна быть минимум через 2 часа от текущего момента.
     */
    @NotNull(message = "Дата события обязательна")
    @JsonFormat(pattern = JacksonConfig.DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    /**
     * Местоположение события.
     */
    @NotNull(message = "Местоположение обязательно")
    private LocationDto location;

    /**
     * Флаг платности участия. По умолчанию false.
     */
    private Boolean paid = false;

    /**
     * Лимит участников. 0 — без ограничений. По умолчанию 0.
     */
    @PositiveOrZero
    private Integer participantLimit = 0;

    /**
     * Флаг пре-модерации заявок. По умолчанию true.
     */
    private Boolean requestModeration = true;

    /**
     * Заголовок события. От 3 до 120 символов.
     */
    @NotBlank(message = "Заголовок не может быть пустым")
    @Size(min = 3, max = 120, message = "Заголовок должен быть от 3 до 120 символов")
    private String title;
}