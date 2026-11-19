package ru.practicum.mainservice.requests.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.common.enums.RequestStatus;

import java.util.List;

/**
 * DTO для запроса на изменение статуса заявок на участие в событии.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    /**
     * Список идентификаторов заявок, статус которых нужно изменить.
     */
    private List<Long> requestIds;

    /**
     * Новый статус для указанных заявок (CONFIRMED или REJECTED).
     */
    private RequestStatus status;
}