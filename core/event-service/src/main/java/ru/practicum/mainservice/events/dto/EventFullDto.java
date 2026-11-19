package ru.practicum.mainservice.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import ru.practicum.common.config.JacksonConfig;
import ru.practicum.common.dto.CategoryDto;
import ru.practicum.common.dto.LocationDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.enums.EventState;

import java.time.LocalDateTime;

/**
 * DTO для полной информации о событии.
 * Используется в административных и приватных запросах.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class EventFullDto {

    /**
     * Идентификатор события.
     */
    private Long id;

    /**
     * Краткое описание события (аннотация).
     */
    private String annotation;

    /**
     * Полное описание события.
     */
    private String description;

    /**
     * Заголовок события.
     */
    private String title;

    /**
     * Категория события.
     */
    private CategoryDto category;

    /**
     * Краткая информация об инициаторе.
     */
    private UserShortDto initiator;

    /**
     * Местоположение события.
     */
    private LocationDto location;

    /**
     * Флаг платности участия.
     */
    private Boolean paid;

    /**
     * Лимит участников. 0 — без ограничений.
     */
    private Integer participantLimit;

    /**
     * Флаг необходимости пре-модерации заявок.
     */
    private Boolean requestModeration;

    /**
     * Количество подтвержденных заявок.
     */
    private Long confirmedRequests;

    /**
     * Количество просмотров.
     */
    private Long views;

    /**
     * Дата и время проведения события.
     */
    @JsonFormat(pattern = JacksonConfig.DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    /**
     * Дата и время создания события.
     */
    @JsonFormat(pattern = JacksonConfig.DATE_TIME_FORMAT)
    private LocalDateTime createdOn;

    /**
     * Дата и время публикации события.
     */
    @JsonFormat(pattern = JacksonConfig.DATE_TIME_FORMAT)
    private LocalDateTime publishedOn;

    /**
     * Текущий статус события.
     */
    private EventState state;
}