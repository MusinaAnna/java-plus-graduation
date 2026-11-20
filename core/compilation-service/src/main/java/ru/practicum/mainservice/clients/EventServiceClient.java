package ru.practicum.mainservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.mainservice.config.FeignClientErrorConfiguration;
import ru.practicum.mainservice.clients.fallback.EventServiceClientFallbackFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.common.dto.EventInternalDto;
import ru.practicum.common.dto.EventShortDto;

import java.util.List;

@FeignClient(name = "event-service", path = "/internal/events", configuration = FeignClientErrorConfiguration.class, fallbackFactory = EventServiceClientFallbackFactory.class)
public interface EventServiceClient {
    @GetMapping("/{eventId}")
    EventInternalDto getEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/short")
    List<EventShortDto> getEventShorts(@RequestParam("ids") List<Long> ids);

    @GetMapping("/exists-by-category/{categoryId}")
    boolean existsByCategoryId(@PathVariable("categoryId") Long categoryId);
}
