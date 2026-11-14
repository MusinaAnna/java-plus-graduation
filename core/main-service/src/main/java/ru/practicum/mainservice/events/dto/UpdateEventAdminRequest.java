package ru.practicum.mainservice.events.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.common.config.JacksonConfig;
import ru.practicum.common.dto.LocationDto;
import ru.practicum.common.enums.AdminStateAction;

import java.time.LocalDateTime;

/**
 * DTO для обновления события администратором.
 * Все поля опциональны.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateEventAdminRequest {

    /**
     * Новая аннотация. От 20 до 2000 символов.
     */
    @Size(min = 20, max = 2000, message = "Аннотация должна быть от 20 до 2000 символов")
    private String annotation;

    /**
     * Новая категория.
     */
    private Long category;

    /**
     * Новое описание. От 20 до 7000 символов.
     */
    @Size(min = 20, max = 7000, message = "Описание должно быть от 20 до 7000 символов")
    private String description;

    /**
     * Новая дата проведения. Должна быть в будущем.
     */
    @Future(message = "Дата события должна быть в будущем")
    @JsonFormat(pattern = JacksonConfig.DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    /**
     * Новое местоположение.
     */
    private LocationDto location;

    /**
     * Новый флаг платности.
     */
    private Boolean paid;

    /**
     * Новый лимит участников.
     */
    private Integer participantLimit;

    /**
     * Новый флаг пре-модерации заявок.
     */
    private Boolean requestModeration;

    /**
     * Действие администратора над статусом события.
     */
    private AdminStateAction stateAction;

    /**
     * Новый заголовок. От 3 до 120 символов.
     */
    @Size(min = 3, max = 120, message = "Заголовок должен быть от 3 до 120 символов")
    private String title;
}