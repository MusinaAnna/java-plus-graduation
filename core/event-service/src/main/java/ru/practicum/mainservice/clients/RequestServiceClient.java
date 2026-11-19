package ru.practicum.mainservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.mainservice.config.FeignClientErrorConfiguration;
import ru.practicum.mainservice.clients.fallback.RequestServiceClientFallbackFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", path = "/internal/requests", configuration = FeignClientErrorConfiguration.class, fallbackFactory = RequestServiceClientFallbackFactory.class)
public interface RequestServiceClient {
    @GetMapping("/confirmed-counts")
    Map<Long, Long> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/confirmed-count")
    long getConfirmedCount(@RequestParam("eventId") Long eventId);
}
