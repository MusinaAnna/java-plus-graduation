package ru.practicum.mainservice.events.service;

import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.mainservice.events.dto.AdminEventQuery;
import ru.practicum.mainservice.events.dto.PublicEventQuery;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventAdminRequest;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;

import java.util.List;

public interface EventService {
    List<EventFullDto> getAdminEvents(AdminEventQuery query);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto createEvent(Long userId, NewEventDto dto);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventShortDto> getPublicEvents(PublicEventQuery query);

    EventFullDto getPublicEvent(Long eventId);

    List<EventShortDto> getRecommendedEvents(Long userId, int size);

    void likeEvent(Long userId, Long eventId);

    EventInternalDto getInternalEvent(Long eventId);

    List<EventShortDto> getInternalEventShorts(List<Long> eventIds);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByInitiatorId(Long initiatorId);
}
