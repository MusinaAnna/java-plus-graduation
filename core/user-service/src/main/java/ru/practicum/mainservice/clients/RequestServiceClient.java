package ru.practicum.mainservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.common.feign.FeignClientErrorConfiguration;
import ru.practicum.mainservice.clients.fallback.RequestServiceClientFallbackFactory;

@FeignClient(name = "request-service", path = "/internal/requests", configuration = FeignClientErrorConfiguration.class,
        fallbackFactory = RequestServiceClientFallbackFactory.class)
public interface RequestServiceClient {
    @GetMapping("/exists-by-requester/{userId}")
    boolean hasRequests(@PathVariable("userId") Long userId);
}
