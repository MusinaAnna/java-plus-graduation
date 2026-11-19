package ru.practicum.mainservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.mainservice.events.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class EventInternalController {
    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventInternalDto getEvent(@PathVariable("eventId") Long eventId) {
        return eventService.getInternalEvent(eventId);
    }

    @GetMapping("/short")
    public List<EventShortDto> getEventShorts(@RequestParam("ids") List<Long> ids) {
        return eventService.getInternalEventShorts(ids);
    }

    @GetMapping("/exists-by-category/{categoryId}")
    public boolean existsByCategoryId(@PathVariable("categoryId") Long categoryId) {
        return eventService.existsByCategoryId(categoryId);
    }

    @GetMapping("/exists-by-initiator/{userId}")
    public boolean existsByInitiatorId(@PathVariable("userId") Long userId) {
        return eventService.existsByInitiatorId(userId);
    }
}
