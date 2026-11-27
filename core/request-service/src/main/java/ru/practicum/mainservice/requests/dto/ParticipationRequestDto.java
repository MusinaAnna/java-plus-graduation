package ru.practicum.mainservice.requests.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.common.enums.RequestStatus;

import java.time.LocalDateTime;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

/**
 * DTO для информации о запросе пользователя на участие в событии.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {

    /**
     * Идентификатор запроса.
     */
    private Long id;

    /**
     * Идентификатор события, на которое подается запрос.
     */
    private Long event;

    /**
     * Идентификатор пользователя, подавшего запрос.
     */
    private Long requester;

    /**
     * Текущий статус запроса.
     */
    private RequestStatus status;

    /**
     * Дата и время создания запроса.
     */
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime created;
}