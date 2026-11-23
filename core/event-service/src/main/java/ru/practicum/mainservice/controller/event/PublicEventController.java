package ru.practicum.mainservice.controller.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.dto.EndpointHit;
import ru.practicum.mainservice.controller.api.event.PublicEventControllerApi;
import ru.practicum.mainservice.events.dto.PublicEventQuery;
import ru.practicum.mainservice.events.dto.EventFullDto;
import ru.practicum.mainservice.events.service.EventService;
import ru.practicum.statistics.client.controller.StatsClient;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController implements PublicEventControllerApi {
    private static final String APP_NAME = "ewm-main-service";

    private final EventService eventService;
    private final StatsClient statsClient;

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
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        log.info("GET /events with params: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, " +
                        "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        List<EventShortDto> events = eventService.getPublicEvents(new PublicEventQuery(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size));
        sendHit(request);
        return events;
    }

    @Override
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /events/{}", id);
        EventFullDto event = eventService.getPublicEvent(id);
        sendHit(request);
        return event;
    }

    private void sendHit(HttpServletRequest request) {
        statsClient.sendHit(new EndpointHit(
                APP_NAME,
                request.getRequestURI(),
                resolveClientIp(request),
                LocalDateTime.now()
        ));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] addresses = forwardedFor.split(",");
            String clientAddress = addresses[0].trim();
            if (!clientAddress.isBlank()) {
                return clientAddress;
            }
        }
        return request.getRemoteAddr();
    }
}
