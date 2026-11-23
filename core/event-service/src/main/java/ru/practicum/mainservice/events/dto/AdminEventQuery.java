package ru.practicum.mainservice.events.dto;

import ru.practicum.common.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

/** Search filters and pagination passed from the HTTP layer to the domain service. */
public record AdminEventQuery(List<Long> users, List<EventState> states, List<Long> categories,
                              LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
}
