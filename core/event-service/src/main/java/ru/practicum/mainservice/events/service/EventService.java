package ru.practicum.mainservice.events.service;

import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states,
                                      List<Long> categories, LocalDateTime rangeStart,
                                      LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto createEvent(Long userId, NewEventDto dto);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventShortDto> getPublicEvents(String text, List<Long> categories,
                                        Boolean paid, LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd, Boolean onlyAvailable,
                                        String sort, int from, int size);

    EventFullDto getPublicEvent(Long eventId);

    EventInternalDto getInternalEvent(Long eventId);

    List<EventShortDto> getInternalEventShorts(List<Long> eventIds);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByInitiatorId(Long initiatorId);
}
