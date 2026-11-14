package ru.practicum.mainservice.controller.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.mainservice.controller.api.event.PrivateEventControllerApi;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.NewEventDto;
import ru.practicum.mainservice.events.dto.UpdateEventUserRequest;
import ru.practicum.mainservice.events.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController implements PrivateEventControllerApi {

    private final EventService eventService;

    @Override
    public List<EventShortDto> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /users/{}/events with from={}, size={}", userId, from, size);
        return eventService.getUserEvents(userId, from, size);
    }

    @Override
    public EventFullDto createEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto dto) {

        log.info("POST /users/{}/events with request: {}", userId, dto);
        return eventService.createEvent(userId, dto);
    }

    @Override
    public EventFullDto getEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("GET /users/{}/events/{}", userId, eventId);
        return eventService.getUserEvent(userId, eventId);
    }

    @Override
    public EventFullDto updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest request) {

        log.info("PATCH /users/{}/events/{} with request: {}", userId, eventId, request);
        return eventService.updateUserEvent(userId, eventId, request);
    }
}
