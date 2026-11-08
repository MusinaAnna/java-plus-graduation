package ru.practicum.mainservice.requests.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для результата обновления статусов заявок на участие.
 * Содержит списки подтвержденных и отклоненных заявок.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateResult {

    /**
     * Список заявок, успешно подтвержденных.
     */
    private List<ParticipationRequestDto> confirmedRequests;

    /**
     * Список заявок, отклоненных.
     */
    private List<ParticipationRequestDto> rejectedRequests;
}