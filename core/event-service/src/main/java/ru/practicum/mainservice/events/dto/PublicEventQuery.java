package ru.practicum.mainservice.events.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Public event search criteria, independent of Spring MVC annotations. */
public record PublicEventQuery(String text, List<Long> categories, Boolean paid,
                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                               String sort, int from, int size) {
}
