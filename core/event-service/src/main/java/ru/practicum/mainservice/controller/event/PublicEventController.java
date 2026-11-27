package ru.practicum.mainservice.controller.event;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.mainservice.controller.api.event.PublicEventControllerApi;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.dto.PublicEventQuery;
import ru.practicum.mainservice.events.service.EventService;
import ru.practicum.statistics.client.grpc.UserActionGrpcClient;
import ru.practicum.common.exception.ServiceUnavailableException;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController implements PublicEventControllerApi {

    private final EventService eventService;
    @Autowired
    private UserActionGrpcClient userActionClient;

    @Override
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /events with params: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, "
                        + "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        return eventService.getPublicEvents(new PublicEventQuery(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size));
    }

    @Override
    public EventFullDto getEvent(
            @PathVariable Long id,
            @RequestHeader("X-EWM-USER-ID") long userId) {

        if (userId <= 0) {
            throw new BadRequestException("Invalid X-EWM-USER-ID header");
        }

        EventFullDto event = eventService.getPublicEvent(id);

        if (userActionClient == null) {
            throw new ServiceUnavailableException("Collector client is not configured");
        }
        userActionClient.sendView(userId, id);

        return event;
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") int size) {

        return eventService.getRecommendedEvents(userId, size);
    }

    @PutMapping("/{id}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(
            @PathVariable Long id,
            @RequestHeader("X-EWM-USER-ID") Long userId) {

        eventService.likeEvent(userId, id);
    }
}
