package ru.practicum.mainservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.common.feign.FeignClientErrorConfiguration;
import ru.practicum.mainservice.clients.fallback.EventServiceClientFallbackFactory;

@FeignClient(name = "event-service", path = "/internal/events", configuration = FeignClientErrorConfiguration.class,
        fallbackFactory = EventServiceClientFallbackFactory.class)
public interface EventServiceClient {
    @GetMapping("/exists-by-initiator/{userId}")
    boolean hasEvents(@PathVariable("userId") Long userId);
}
